package io.github.davamix.workouttimer.audio

import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The speaker on the tablet, which is set to Spanish: it selects an installed English (US) voice that needs no
 * network, and reports when speech ends (TTS-4, TTS-10, TTS-11). Speech is heard during the test.
 */
@RunWith(AndroidJUnit4::class)
class TtsSpeakerDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val speaker = TtsSpeaker(context) { message, _ -> warnings += message }

    @Test
    fun prepareReachesReadyWithAnOfflineEnglishUsVoice() {
        val start = SystemClock.elapsedRealtime()
        val status = runBlocking { speaker.prepare() }
        Log.i(TAG, "prepare() took ${SystemClock.elapsedRealtime() - start} ms, voice ${speaker.voiceName}")
        logEnglishVoices()

        assertEquals(SpeakerStatus.READY, status)
        val voice = checkNotNull(speaker.selectedVoice) { "No voice selected" }
        assertEquals("en", voice.locale.language)
        assertEquals("US", voice.locale.country)
        assertFalse("${voice.name} needs the network", voice.isNetworkConnectionRequired)
        assertEquals(voice.name, speaker.voiceName)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun speakCallsOnDoneOnceSpoken() {
        runBlocking { speaker.prepare() }
        val done = CountDownLatch(1)

        speaker.speak("Jumping jacks") { done.countDown() }

        assertTrue("onDone within 5 s", done.await(5, TimeUnit.SECONDS))
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun stopCutsSpeechOffWithoutCallingOnDone() {
        runBlocking { speaker.prepare() }
        val cutOff = CountDownLatch(1)
        val queued = CountDownLatch(1)

        speaker.speak("This is a long sentence, so that there is time to stop it before it ends.") { cutOff.countDown() }
        speaker.speak("Queued after it.") { queued.countDown() }
        SystemClock.sleep(700)
        speaker.stop()

        assertFalse("onDone of the cut-off speech", cutOff.await(3, TimeUnit.SECONDS))
        assertEquals("onDone of the dropped speech", 1L, queued.count)

        val after = CountDownLatch(1)
        speaker.speak("one") { after.countDown() }
        assertTrue("speaks again after stop", after.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun speakBeforePrepareCallsOnDoneAtOnce() {
        val done = CountDownLatch(1)

        speaker.speak("Not heard") { done.countDown() }

        assertEquals(0L, done.count)
        assertEquals(SpeakerStatus.INITIALIZING, speaker.status.value)
        assertEquals(null, speaker.voiceName)
    }

    /** Logs every English (US) voice the engine lists, for diagnosing the choice. */
    private fun logEnglishVoices() {
        val done = CountDownLatch(1)
        val engine = TextToSpeech(context) { done.countDown() }
        try {
            done.await(5, TimeUnit.SECONDS)
            engine.voices
                .orEmpty()
                .filter { it.locale.language == "en" && it.locale.country == "US" }
                .sortedBy { it.name }
                .forEach {
                    Log.i(
                        TAG,
                        "voice ${it.name}: locale ${it.locale}, quality ${it.quality}, latency ${it.latency}, " +
                            "network ${it.isNetworkConnectionRequired}, features ${it.features}",
                    )
                }
        } finally {
            engine.shutdown()
        }
    }

    private companion object {
        const val TAG = "TtsSpeakerDeviceTest"
    }
}
