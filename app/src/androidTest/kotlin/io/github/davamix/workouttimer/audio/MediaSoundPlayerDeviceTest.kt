package io.github.davamix.workouttimer.audio

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The round-end chime plays on the tablet, and reports when it has finished (SND-1). It is heard during the test. */
@RunWith(AndroidJUnit4::class)
class MediaSoundPlayerDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val warnings: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val player = MediaSoundPlayer(context) { message, _ -> warnings += message }

    @Test
    fun soundLastsAtMost800Milliseconds() {
        val media = MediaPlayer.create(context, R.raw.round_end)
        try {
            assertTrue("Lasts ${media.duration} ms", media.duration in 300..800)
        } finally {
            media.release()
        }
    }

    @Test
    fun playRoundEndCallsOnDoneWhenTheSoundHasFinished() {
        player.prepare()
        val done = CountDownLatch(1)
        val start = SystemClock.elapsedRealtime()

        player.playRoundEnd { done.countDown() }

        assertTrue("onDone within 2 s", done.await(2, TimeUnit.SECONDS))
        val elapsed = SystemClock.elapsedRealtime() - start
        assertTrue("onDone after the sound played, but came after $elapsed ms", elapsed >= 300)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun playRoundEndWorksWithoutPrepareAndCanPlayAgain() {
        repeat(2) {
            val done = CountDownLatch(1)
            player.playRoundEnd { done.countDown() }
            assertTrue("onDone of play ${it + 1} within 2 s", done.await(2, TimeUnit.SECONDS))
        }
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun playingAgainCutsOffThePreviousPlayWhichCountsAsFinished() {
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)

        player.playRoundEnd { first.countDown() }
        SystemClock.sleep(100)
        val cutAt = SystemClock.elapsedRealtime()
        player.playRoundEnd { second.countDown() }

        assertTrue("first onDone at once", first.await(100, TimeUnit.MILLISECONDS))
        assertTrue("second onDone within 2 s", second.await(2, TimeUnit.SECONDS))
        assertTrue("second played in full", SystemClock.elapsedRealtime() - cutAt >= 300)
        assertEquals(emptyList<String>(), warnings)
    }
}
