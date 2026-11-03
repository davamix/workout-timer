package io.github.davamix.workouttimer.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The steps of the manual persistence check in P3 SPEC.md §6 (WM-8, TTS-9). They use the app's real `AppContainer`
 * repositories, so the data lands where the app keeps it.
 *
 * Each step runs only when the `persistenceCheck` instrumentation argument names it, so `connectedDebugAndroidTest`
 * skips them all. Run one with:
 *
 * ```
 * adb shell am instrument -w -e persistenceCheck <step> \
 *     -e class io.github.davamix.workouttimer.data.PersistenceCheck#<step> \
 *     io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 */
@RunWith(AndroidJUnit4::class)
class PersistenceCheck {
    private val container =
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as WorkoutTimerApp).container
    private val requestedStep: String? = InstrumentationRegistry.getArguments().getString("persistenceCheck")

    /** Saves the check workout and turns voice cues off (not the default). */
    @Test
    fun seed() =
        step("seed") {
            container.workoutRepository.save(CHECK_WORKOUT)
            container.settingsRepository.setVoiceCuesEnabled(false)
        }

    /** The check workout and the voice setting are as [seed] left them. Can run any number of times. */
    @Test
    fun verify() =
        step("verify") {
            assertEquals(CHECK_WORKOUT, container.workoutRepository.get(CHECK_WORKOUT.id))
            assertFalse(
                container.settingsRepository.settings
                    .first()
                    .voiceCuesEnabled,
            )
        }

    /** Deletes the check workout and turns voice cues back on. */
    @Test
    fun clean() =
        step("clean") {
            container.workoutRepository.delete(CHECK_WORKOUT.id)
            container.settingsRepository.setVoiceCuesEnabled(true)

            assertNull(container.workoutRepository.get(CHECK_WORKOUT.id))
            assertTrue(
                container.settingsRepository.settings
                    .first()
                    .voiceCuesEnabled,
            )
        }

    private fun step(
        name: String,
        block: suspend () -> Unit,
    ) {
        assumeTrue("Runs only with -e persistenceCheck $name", requestedStep == name)
        runBlocking { block() }
    }

    private companion object {
        /** Two rounds, with own values, linked values and an unused value (the last round's rest). */
        val CHECK_WORKOUT =
            Workout(
                id = "p3-persistence-check",
                name = "P3 persistence check",
                defaultExerciseDurationSeconds = 40,
                defaultExerciseRestSeconds = 15,
                defaultRoundRestSeconds = 60,
                rounds =
                    listOf(
                        Round(
                            id = "p3-check-r1",
                            name = "Warm-up",
                            restSeconds = 30,
                            exercises =
                                listOf(
                                    Exercise(id = "p3-check-e1", name = "Jumping jacks", durationSeconds = 30),
                                    Exercise(id = "p3-check-e2", name = "High knees"),
                                ),
                        ),
                        Round(
                            id = "p3-check-r2",
                            name = "Core",
                            restSeconds = 45,
                            exercises =
                                listOf(
                                    Exercise(id = "p3-check-e3", name = "Plank", durationSeconds = 60, restSeconds = 30),
                                    Exercise(id = "p3-check-e4", name = "Crunches"),
                                ),
                        ),
                    ),
            )
    }
}
