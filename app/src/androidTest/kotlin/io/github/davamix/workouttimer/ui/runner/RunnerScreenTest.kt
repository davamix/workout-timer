package io.github.davamix.workouttimer.ui.runner

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.engine.Clock
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.repository.InMemoryWorkoutRepository
import io.github.davamix.workouttimer.core.settings.InMemorySettingsRepository
import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.ui.completed.WorkoutSummary
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import io.github.davamix.workouttimer.ui.uiExampleWorkout
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The runner screen with a real ViewModel, session and cue player, on in-memory repositories and silent audio, in
 * real time (P7 SPEC §6).
 *
 * With voice cues on, the silent speaker never finishes "Get ready in", so get ready waits up to 5 s for it: the tests
 * pause in that time, and everything after that is paused and doesn't depend on timing.
 */
@RunWith(AndroidJUnit4::class)
class RunnerScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val completed = mutableListOf<WorkoutSummary>()
    private var exits = 0
    private var viewModel: RunnerViewModel? = null

    @After
    fun stopTheSession() {
        viewModel?.viewModelScope?.cancel()
    }

    private fun show(
        workout: Workout = uiExampleWorkout,
        voiceOn: Boolean = true,
    ) {
        val runner =
            compose.runOnUiThread {
                RunnerViewModel(
                    workoutId = workout.id,
                    workoutRepository = InMemoryWorkoutRepository(listOf(workout)),
                    settingsRepository = InMemorySettingsRepository(Settings(voiceCuesEnabled = voiceOn)),
                    clock = Clock { SystemClock.elapsedRealtime() },
                    cuePlayer = silentCuePlayer(),
                    savedStateHandle = SavedStateHandle(),
                )
            }
        viewModel = runner
        compose.setContent {
            WorkoutTimerTheme {
                RunnerScreen(
                    workoutId = workout.id,
                    onCompleted = { completed += it },
                    onExit = { exits++ },
                    viewModel = runner,
                )
            }
        }
    }

    private fun text(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun tag(tag: String) = compose.onNodeWithTag(tag)

    private fun waitForTag(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        compose.waitUntil(timeoutMillis) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun timeShown(): String =
        tag("runner-time")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString { it.text }

    private fun pauseOnceRunning() {
        waitForTag("runner-button-pause")
        tag("runner-button-pause").performClick()
        waitForTag("runner-paused")
    }

    private fun assertPausedAt(
        title: String,
        time: String,
    ) {
        tag("runner-title").assertTextEquals(title)
        tag("runner-time").assertTextEquals(time)
        tag("runner-paused").assertExists()
    }

    @Test
    fun pauseKeepsTheTimeAndResumeCountsOn() {
        show()
        pauseOnceRunning()
        val paused = timeShown()

        SystemClock.sleep(1_500)
        compose.waitForIdle()
        assertEquals(paused, timeShown())

        tag("runner-button-resume").performClick()
        waitForTag("runner-button-pause")
        compose.waitUntil(5_000) { timeShown() != paused }

        tag("runner-button-pause").performClick()
        waitForTag("runner-paused")
        val pausedAgain = timeShown()
        SystemClock.sleep(1_500)
        compose.waitForIdle()
        assertEquals(pausedAgain, timeShown())
    }

    @Test
    fun nextAndPreviousMoveBetweenExercisesWhilePaused() {
        show()
        pauseOnceRunning()
        tag("runner-title").assertTextEquals(text(R.string.runner_get_ready))
        tag("runner-button-previous").assertIsNotEnabled()

        tag("runner-button-next").performClick()
        assertPausedAt("Jumping jacks", "00:30")

        tag("runner-button-next").performClick()
        assertPausedAt("Arm circles", "00:10")

        tag("runner-button-previous").performClick()
        assertPausedAt("Jumping jacks", "00:30")

        tag("runner-button-previous").performClick()
        assertPausedAt("Jumping jacks", "00:30")
    }

    @Test
    fun stopAsksCancelKeepsTheWorkoutAndStopLeaves() {
        show()
        pauseOnceRunning()

        tag("runner-button-stop").performClick()
        tag("runner-stop-cancel").performClick()
        tag("runner-stop-cancel").assertDoesNotExist()
        tag("runner-paused").assertExists()

        tag("runner-button-stop").performClick()
        tag("runner-stop-confirm").performClick()
        compose.waitUntil(5_000) { exits == 1 }
        assertTrue(completed.isEmpty())
    }

    @Test
    fun nextOnTheLastExerciseCompletesWithTheSummary() {
        show()
        pauseOnceRunning()
        repeat(5) { tag("runner-button-next").performClick() }
        tag("runner-title").assertTextEquals("Crunches")

        tag("runner-button-next").performClick()
        compose.waitUntil(5_000) { completed.isNotEmpty() }
        assertEquals(listOf(WorkoutSummary("Example", totalSeconds = 263, roundCount = 2, exerciseCount = 5)), completed)
        assertEquals(0, exits)
    }

    @Test
    fun systemBackPausesThenAsksToStopThenClosesTheDialog() {
        show()
        waitForTag("runner-button-pause")

        pressBack()
        waitForTag("runner-paused")
        tag("runner-stop-confirm").assertDoesNotExist()

        pressBack()
        waitForTag("runner-stop-confirm")

        pressBack()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("runner-stop-confirm").fetchSemanticsNodes().isEmpty() }
        tag("runner-paused").assertExists()
        assertEquals(0, exits)
    }

    @Test
    fun aShortWorkoutRunsToTheCompletedScreenByItself() {
        val short =
            Workout(
                id = "w-short",
                name = "Short",
                defaultExerciseDurationSeconds = 40,
                defaultExerciseRestSeconds = 15,
                defaultRoundRestSeconds = 60,
                rounds = listOf(Round("r-short", "Round 1", exercises = listOf(Exercise("e-short", "Squats", durationSeconds = 1)))),
            )
        show(short, voiceOn = false)

        compose.waitUntil(10_000) { completed.isNotEmpty() }
        assertEquals(listOf(WorkoutSummary("Short", totalSeconds = 4, roundCount = 1, exerciseCount = 1)), completed)
    }
}
