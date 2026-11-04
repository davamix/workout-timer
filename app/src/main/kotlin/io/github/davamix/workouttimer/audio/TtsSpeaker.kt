package io.github.davamix.workouttimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

/**
 * A [Speaker] on the platform [TextToSpeech], with the device's default engine (Google Speech Services on the tablet)
 * and an English (United States) voice, whatever the device language (plan D-9, TTS-11).
 *
 * The voice is the best installed English (US) one that doesn't need the network, so cues work offline (TTS-4). It
 * speaks at the media volume (specification §5.3). It lives as long as the app, so it's never shut down.
 *
 * Before it's READY, it warms the engine up: it synthesises [warmUpTexts] into a temporary file in the app's cache
 * folder, then deletes the file. Otherwise, on the tablet, the first phrase after the engine connects starts 3–4 s
 * late, and every new phrase ~250 ms late. A phrase synthesised before starts within ~30 ms.
 *
 * Problems never throw: they go to [onWarning], make the status UNAVAILABLE, and the next [prepare] reconnects. A
 * warm-up that fails only makes speech start later, so the speaker becomes READY anyway.
 *
 * @param warmUpTexts The phrases to warm up with: those that must start on time, such as the countdown.
 * @param onWarning Called with a message, and the error if there is one, for every problem.
 */
class TtsSpeaker(
    context: Context,
    private val warmUpTexts: List<String>,
    private val onWarning: (message: String, error: Throwable?) -> Unit = { _, _ -> },
) : Speaker {
    private val context = context.applicationContext

    private val mutableStatus = MutableStateFlow(SpeakerStatus.INITIALIZING)
    override val status: StateFlow<SpeakerStatus> = mutableStatus.asStateFlow()

    /** The selected voice, once the engine has one. The speaker may still be warming up. */
    @Volatile
    var selectedVoice: Voice? = null
        private set

    override val voiceName: String?
        get() = if (mutableStatus.value == SpeakerStatus.READY) selectedVoice?.name else null

    // Guarded by lock: the engine and its generation, the onDone callbacks by utterance ID, the next ID, and the
    // warm-up phrases still to finish.
    private val lock = Any()
    private var engine: TextToSpeech? = null
    private var engineGeneration = 0
    private val callbacks = HashMap<String, () -> Unit>()
    private var nextUtterance = 0L
    private val warmingUp = HashSet<String>()

    override suspend fun prepare(timeoutMillis: Long): SpeakerStatus {
        val dropped =
            synchronized(lock) {
                when {
                    engine == null -> {
                        connectLocked()
                        emptyList()
                    }
                    mutableStatus.value == SpeakerStatus.UNAVAILABLE -> {
                        engine?.shutdown()
                        val pending = callbacks.values.toList()
                        callbacks.clear()
                        connectLocked()
                        pending
                    }
                    else -> emptyList()
                }
            }
        // Speech queued on the failed engine will never be heard.
        dropped.forEach { it() }
        withTimeoutOrNull(timeoutMillis) { mutableStatus.first { it != SpeakerStatus.INITIALIZING } }
        return mutableStatus.value
    }

    override fun speak(
        text: String,
        onDone: () -> Unit,
    ) {
        val queued =
            synchronized(lock) {
                val engine = engine
                if (engine == null || mutableStatus.value != SpeakerStatus.READY) return@synchronized false
                val id = "cue-${nextUtterance++}"
                callbacks[id] = onDone
                val result =
                    try {
                        engine.speak(text, TextToSpeech.QUEUE_ADD, null, id)
                    } catch (e: RuntimeException) {
                        onWarning("Speaking failed", e)
                        TextToSpeech.ERROR
                    }
                if (result != TextToSpeech.SUCCESS) {
                    callbacks.remove(id)
                    mutableStatus.value = SpeakerStatus.UNAVAILABLE
                    onWarning("Speaking failed: speak() returned $result", null)
                }
                result == TextToSpeech.SUCCESS
            }
        if (!queued) onDone()
    }

    override fun stop() {
        synchronized(lock) {
            callbacks.clear()
            // While warming up, nothing else can be queued, as speak() needs READY. Stopping would only cut the
            // warm-up short.
            if (warmingUp.isNotEmpty()) return
            try {
                engine?.stop()
            } catch (e: RuntimeException) {
                onWarning("Stopping speech failed", e)
            }
        }
    }

    /** Creates a new engine. Its initialisation finishes in [onInit], usually later on the main thread. */
    private fun connectLocked() {
        mutableStatus.value = SpeakerStatus.INITIALIZING
        selectedVoice = null
        engine = null
        // Callbacks from the old engine's warm-up are ignored: the IDs carry the engine's generation.
        warmingUp.clear()
        val generation = ++engineGeneration
        // On failure, TextToSpeech may call the listener before its constructor returns: onInit handles that, and
        // the next prepare() shuts the engine down and connects again.
        val created = TextToSpeech(context) { result -> onInit(generation, result) }
        created.setOnUtteranceProgressListener(progressListener)
        engine = created
    }

    private fun onInit(
        generation: Int,
        result: Int,
    ) {
        synchronized(lock) {
            if (generation != engineGeneration) return // from an engine that has been replaced
            val engine = engine
            if (result != TextToSpeech.SUCCESS || engine == null) {
                onWarning("The TTS engine didn't start: onInit returned $result", null)
                mutableStatus.value = SpeakerStatus.UNAVAILABLE
                return
            }
            val configured =
                try {
                    configure(engine)
                } catch (e: RuntimeException) {
                    onWarning("Can't set up the TTS engine", e)
                    SpeakerStatus.UNAVAILABLE
                }
            mutableStatus.value =
                if (configured == SpeakerStatus.READY) startWarmUpLocked(engine, generation) else configured
        }
    }

    /** Sets the audio attributes and the English (US) voice; returns the resulting status. */
    private fun configure(engine: TextToSpeech): SpeakerStatus {
        engine.setAudioAttributes(AUDIO_ATTRIBUTES)

        val offline = bestOfflineUsVoice(engine.voices.orEmpty())
        if (offline != null && engine.setVoice(offline) == TextToSpeech.SUCCESS) {
            selectedVoice = offline
            return SpeakerStatus.READY
        }

        val language = engine.setLanguage(Locale.US)
        if (language < TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            onWarning("No English (US) voice: setLanguage returned $language", null)
            return SpeakerStatus.UNAVAILABLE
        }
        selectedVoice = engine.voice
        onWarning("No offline English (US) voice is listed; using ${engine.voice?.name}, which may need the network", null)
        return SpeakerStatus.READY
    }

    /**
     * Queues [warmUpTexts] to be synthesised into the warm-up file, one after another. Returns INITIALIZING while
     * they're under way, or READY if none could be queued.
     */
    private fun startWarmUpLocked(
        engine: TextToSpeech,
        generation: Int,
    ): SpeakerStatus {
        val file = warmUpFile()
        warmUpTexts.forEachIndexed { index, text ->
            val id = "$WARM_UP_ID_PREFIX$generation-$index"
            val result =
                try {
                    engine.synthesizeToFile(text, null, file, id)
                } catch (e: RuntimeException) {
                    onWarning("Warming up failed", e)
                    TextToSpeech.ERROR
                }
            if (result == TextToSpeech.SUCCESS) {
                warmingUp += id
            } else {
                onWarning("Warming up failed: synthesizeToFile returned $result", null)
            }
        }
        if (warmingUp.isNotEmpty()) return SpeakerStatus.INITIALIZING
        file.delete()
        return SpeakerStatus.READY
    }

    /** Whether [utteranceId] is a warm-up phrase. When it's the last one to finish, the speaker becomes READY. */
    private fun finishWarmUpLocked(utteranceId: String?): Boolean {
        if (!warmingUp.remove(utteranceId)) return false
        if (warmingUp.isEmpty()) {
            warmUpFile().delete()
            mutableStatus.value = SpeakerStatus.READY
        }
        return true
    }

    private fun warmUpFile() = File(context.cacheDir, WARM_UP_FILE_NAME)

    private val progressListener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) = finished(utteranceId)

            @Deprecated("Superseded by onError(String, Int)")
            override fun onError(utteranceId: String?) = finished(utteranceId)

            override fun onError(
                utteranceId: String?,
                errorCode: Int,
            ) {
                onWarning("Speaking failed with error $errorCode: $utteranceId", null)
                finished(utteranceId)
            }

            // A cue cut off by stop(), which has already dropped its callback. A warm-up phrase, which stop() leaves
            // alone, counts as done.
            override fun onStop(
                utteranceId: String?,
                interrupted: Boolean,
            ) {
                synchronized(lock) { finishWarmUpLocked(utteranceId) }
            }
        }

    private fun finished(utteranceId: String?) {
        val onDone =
            synchronized(lock) {
                if (finishWarmUpLocked(utteranceId)) null else callbacks.remove(utteranceId)
            }
        onDone?.invoke()
    }

    internal companion object {
        /** The temporary file in the app's cache folder that the warm-up writes. It's deleted once the warm-up is done. */
        const val WARM_UP_FILE_NAME = "tts-warm-up.wav"

        private const val WARM_UP_ID_PREFIX = "warm-up-"

        val AUDIO_ATTRIBUTES: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

        /**
         * The best voice among [voices] for English (United States) that is installed and needs no network: highest
         * quality, then lowest latency, then by name, so the choice is stable. `null` if there's none.
         */
        fun bestOfflineUsVoice(voices: Collection<Voice>): Voice? =
            voices
                .filter { voice ->
                    voice.locale.isUnitedStatesEnglish() &&
                        !voice.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
                }.sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.latency }.thenBy { it.name })
                .firstOrNull()

        /** Engines name locales with two-letter or three-letter codes. */
        private fun Locale.isUnitedStatesEnglish(): Boolean = language in setOf("en", "eng") && country in setOf("US", "USA")
    }
}
