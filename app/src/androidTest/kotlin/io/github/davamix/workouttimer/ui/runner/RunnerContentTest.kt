package io.github.davamix.workouttimer.ui.runner

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.engine.SessionState
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.ui.assertTouchTargetsAtLeast
import io.github.davamix.workouttimer.ui.theme.PhaseColors
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import io.github.davamix.workouttimer.ui.uiExampleWorkout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The runner's content (P7 SPEC §4.3.2, §4.3.3, §4.3.9) with fixed states of the §4.4 example. */
@RunWith(AndroidJUnit4::class)
class RunnerContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val timeline = Timeline.of(uiExampleWorkout)
    private val actions = RecordingActions()

    private fun show(state: RunnerState) {
        compose.setContent { WorkoutTimerTheme { RunnerContent(state, actions) } }
    }

    /** The example's phase [index] with [remaining] seconds left, running or paused. */
    private fun active(
        index: Int,
        remaining: Int = timeline.phases[index].durationSeconds,
        paused: Boolean = false,
        confirmingStop: Boolean = false,
    ) = RunnerState.Active(
        SessionState(
            status = if (paused) SessionStatus.PAUSED else SessionStatus.RUNNING,
            phaseIndex = index,
            phase = timeline.phases[index],
            remainingSeconds = remaining,
            awaitingAnnouncement = false,
            canGoPrevious = paused && index > 0,
        ).toRunnerView(),
        confirmingStop = confirmingStop,
    )

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun tag(tag: String) = compose.onNodeWithTag(tag)

    private fun assertAbsent(vararg tags: String) {
        tags.forEach { tag(it).assertDoesNotExist() }
    }

    /** The background colour, read from a pixel near the top-left corner. */
    private fun assertBackground(expected: Color) {
        val pixel = compose.onRoot().captureToImage().toPixelMap()[2, 2]
        assertEquals(expected.toArgb(), pixel.toArgb())
    }

    // What each look shows (§4.3.2)

    @Test
    fun getReadyShowsTheFirstRoundTheCountdownAndTheFirstExercise() {
        show(active(0))

        tag("runner-round").assertTextEquals(text(R.string.runner_round, 1, 2, "Warm-up"))
        tag("runner-title").assertTextEquals(text(R.string.runner_get_ready))
        tag("runner-time").assertTextEquals("00:03")
        tag("runner-next-exercise").assertTextEquals(text(R.string.runner_next_exercise, "Jumping jacks", 30))
        assertAbsent("runner-exercise-number", "runner-next-round", "runner-paused")
        assertBackground(PhaseColors.GetReady)
    }

    @Test
    fun anExerciseShowsItsNameAndItsPlaceInTheRound() {
        show(active(1, remaining = 27))

        tag("runner-round").assertTextEquals(text(R.string.runner_round, 1, 2, "Warm-up"))
        tag("runner-exercise-number").assertTextEquals(text(R.string.runner_exercise, 1, 3))
        tag("runner-title").assertTextEquals("Jumping jacks")
        tag("runner-time").assertTextEquals("00:27")
        assertAbsent("runner-next-exercise", "runner-next-round", "runner-paused")
        assertBackground(PhaseColors.Exercise)
    }

    @Test
    fun anExerciseRestShowsRestAndTheNextExercise() {
        show(active(2, remaining = 12))

        tag("runner-title").assertTextEquals(text(R.string.runner_rest))
        tag("runner-time").assertTextEquals("00:12")
        tag("runner-next-exercise").assertTextEquals(text(R.string.runner_next_exercise, "Arm circles", 10))
        assertAbsent("runner-exercise-number", "runner-next-round")
        assertBackground(PhaseColors.Rest)
    }

    @Test
    fun aRoundRestShowsTheRoundThatEndedTheNextRoundAndItsFirstExercise() {
        show(active(6))

        tag("runner-round").assertTextEquals(text(R.string.runner_round, 1, 2, "Warm-up"))
        tag("runner-title").assertTextEquals(text(R.string.runner_rest))
        tag("runner-time").assertTextEquals("00:30")
        tag("runner-next-round").assertTextEquals(text(R.string.runner_next_round, "Core"))
        tag("runner-next-exercise").assertTextEquals(text(R.string.runner_next_exercise, "Plank", 60))
        assertBackground(PhaseColors.Rest)
    }

    @Test
    fun pausedShowsPausedOnItsOwnColourAndKeepsThePhase() {
        show(active(6, remaining = 20, paused = true))

        tag("runner-paused").assertTextEquals(text(R.string.runner_paused))
        tag("runner-title").assertTextEquals(text(R.string.runner_rest))
        tag("runner-time").assertTextEquals("00:20")
        tag("runner-next-round").assertTextEquals(text(R.string.runner_next_round, "Core"))
        assertBackground(PhaseColors.Paused)
    }

    // Buttons (UI-10, UI-11, §4.3.3)

    @Test
    fun runningShowsOnlyPause() {
        show(active(1))

        tag("runner-button-pause").performClick()
        assertAbsent("runner-button-resume", "runner-button-previous", "runner-button-next", "runner-button-stop")
        assertEquals(listOf("pause"), actions.calls)
    }

    @Test
    fun pausedShowsResumePreviousNextAndStopEachCallingItsAction() {
        show(active(3, paused = true))
        assertAbsent("runner-button-pause")

        tag("runner-button-resume").performClick()
        tag("runner-button-previous").assertIsEnabled().performClick()
        tag("runner-button-next").performClick()
        tag("runner-button-stop").performClick()
        assertEquals(listOf("resume", "previous", "next", "requestStop"), actions.calls)
    }

    @Test
    fun previousIsDisabledWhenPausedInGetReady() {
        show(active(0, remaining = 2, paused = true))

        tag("runner-button-previous").assertIsNotEnabled()
        tag("runner-button-next").assertIsEnabled()
    }

    @Test
    fun theStopDialogAsksAndItsButtonsCallTheirActions() {
        show(active(1, paused = true, confirmingStop = true))
        compose.onNode(hasText(text(R.string.runner_stop_title)) and hasAnyAncestor(isDialog())).assertExists()

        tag("runner-stop-cancel").performClick()
        tag("runner-stop-confirm").performClick()
        assertEquals(listOf("cancelStop", "confirmStop"), actions.calls)
    }

    // Starting and unavailable (§4.3.1)

    @Test
    fun startingShowsTheWorkoutNameOnGetReadyColour() {
        show(RunnerState.Starting("Example"))

        compose.onNodeWithText("Example").assertExists()
        compose.onNodeWithText(text(R.string.runner_starting)).assertExists()
        assertAbsent("runner-button-pause")
        assertBackground(PhaseColors.GetReady)
    }

    @Test
    fun unavailableSaysSoAndGoesBack() {
        show(RunnerState.Unavailable)

        compose.onNodeWithText(text(R.string.runner_unavailable)).assertExists()
        compose.onNodeWithText(text(R.string.runner_back_to_list)).performClick()
        assertEquals(listOf("back"), actions.calls)
    }

    // Touch targets (NF-6)

    @Test
    fun runningButtonsAreLarge() {
        show(active(1))
        compose.assertTouchTargetsAtLeast(72.dp)
    }

    @Test
    fun pausedButtonsAreLarge() {
        show(active(6, paused = true))
        compose.assertTouchTargetsAtLeast(72.dp)
    }

    @Test
    fun theStopDialogButtonsAreLargeEnough() {
        show(active(1, paused = true, confirmingStop = true))
        compose.assertTouchTargetsAtLeast(56.dp)
    }

    /** Records each action by name. */
    private class RecordingActions : RunnerActions {
        val calls = mutableListOf<String>()

        override fun pause() {
            calls += "pause"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun next() {
            calls += "next"
        }

        override fun previous() {
            calls += "previous"
        }

        override fun requestStop() {
            calls += "requestStop"
        }

        override fun confirmStop() {
            calls += "confirmStop"
        }

        override fun cancelStop() {
            calls += "cancelStop"
        }

        override fun back() {
            calls += "back"
        }
    }
}
