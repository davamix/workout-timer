package io.github.davamix.workouttimer.audio

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.core.cues.Utterance
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** The spoken phrases are English whatever the device language (TTS-11). */
@RunWith(AndroidJUnit4::class)
class SpokenTextDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun phrasesAreEnglishWithASpanishConfiguration() {
        val spanish = Configuration(context.resources.configuration).apply { setLocale(Locale("es", "ES")) }
        val text = SpokenText(context.createConfigurationContext(spanish).resources)

        assertEquals("Get ready in", text.of(Utterance.GetReadyIn))
        assertEquals("three", text.of(Utterance.Count(3)))
        assertEquals("two", text.of(Utterance.Count(2)))
        assertEquals("one", text.of(Utterance.Count(1)))
        assertEquals("Workout complete. Well done!", text.of(Utterance.WorkoutComplete))
    }

    @Test
    fun exerciseNamesAreSpokenAsWritten() {
        val text = SpokenText(context.resources)

        assertEquals("Sentadillas, wide", text.of(Utterance.ExerciseName("Sentadillas, wide")))
    }
}
