package io.github.davamix.workouttimer.audio

import io.github.davamix.workouttimer.core.cues.Utterance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Fakes for the audio tests. Both write to one shared log, so a test can check the order of speech and sound.

/** A log of what the fakes did, each entry prefixed with the time from [now], e.g. `1500 speak three`. */
class AudioLog(
    private val now: () -> Long = { 0 },
) {
    val entries = mutableListOf<String>()

    fun add(entry: String) {
        entries += "${now()} $entry"
    }

    /** The entries without their times. */
    val actions: List<String> get() = entries.map { it.substringAfter(' ') }
}

/**
 * A [Speaker] that logs `speak <text>`, `unheard <text>` (when not READY, or [failSpeaking]) and `stop`. Speech
 * finishes when the test calls [finish], or after [durationOf] its text in [scope]'s virtual time, if both are set.
 */
class FakeSpeaker(
    private val log: AudioLog,
    initialStatus: SpeakerStatus = SpeakerStatus.READY,
    private val scope: CoroutineScope? = null,
    private val durationOf: (String) -> Long? = { null },
) : Speaker {
    override val status = MutableStateFlow(initialStatus)
    override val voiceName: String? = "fake"

    var failSpeaking = false
    var prepareCalls = 0
        private set

    /** Speech handed over and not finished yet, oldest first. */
    private val pending = mutableListOf<Pair<String, () -> Unit>>()

    override suspend fun prepare(timeoutMillis: Long): SpeakerStatus {
        prepareCalls++
        return status.value
    }

    override fun speak(
        text: String,
        onDone: () -> Unit,
    ) {
        if (status.value != SpeakerStatus.READY || failSpeaking) {
            log.add("unheard $text")
            onDone()
            return
        }
        log.add("speak $text")
        val entry = text to onDone
        pending += entry
        val duration = durationOf(text)
        if (scope != null && duration != null) {
            scope.launch {
                delay(duration)
                if (pending.remove(entry)) onDone()
            }
        }
    }

    override fun stop() {
        log.add("stop")
        pending.clear()
    }

    /** Finishes the oldest pending speech of [text], calling its `onDone`. */
    fun finish(text: String) {
        val entry = pending.first { it.first == text }
        pending.remove(entry)
        entry.second()
    }
}

/** A [SoundPlayer] that logs `sound`, and finishes when the test calls [finish]. */
class FakeSoundPlayer(
    private val log: AudioLog,
) : SoundPlayer {
    var prepareCalls = 0
        private set

    private var pending: (() -> Unit)? = null

    override fun prepare() {
        prepareCalls++
    }

    override fun playRoundEnd(onDone: () -> Unit) {
        log.add("sound")
        pending = onDone
    }

    fun finish() {
        pending?.invoke()
        pending = null
    }
}

/** English text for the tests, the same as `strings_audio.xml`, which JVM tests can't read. */
fun testText(utterance: Utterance): String =
    when (utterance) {
        Utterance.GetReadyIn -> "Get ready in"
        is Utterance.Count -> listOf("one", "two", "three")[utterance.number - 1]
        is Utterance.ExerciseName -> utterance.name
        Utterance.WorkoutComplete -> "Workout complete. Well done!"
    }
