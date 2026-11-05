package io.github.davamix.workouttimer.ui.completed

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.assertTouchTargetsAtLeast
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The completed screen (EX-6, P7 SPEC §4.3.8). */
@RunWith(AndroidJUnit4::class)
class CompletedScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var backClicks = 0

    private fun show(summary: WorkoutSummary) {
        compose.setContent { WorkoutTimerTheme { CompletedScreen(summary, onBackToList = { backClicks++ }) } }
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Test
    fun showsWellDoneTheWorkoutAndItsPlannedSummary() {
        show(WorkoutSummary("Example", totalSeconds = 263, roundCount = 2, exerciseCount = 5))

        compose.onNodeWithTag("completed-title").assertTextEquals(text(R.string.completed_title))
        compose.onNodeWithTag("completed-workout").assertTextEquals(text(R.string.completed_workout, "Example"))
        compose.onNodeWithTag("completed-summary").assertTextEquals("Total time 4:23 · 2 rounds · 5 exercises")
    }

    @Test
    fun oneRoundAndOneExerciseAreSingular() {
        show(WorkoutSummary("Single", totalSeconds = 43, roundCount = 1, exerciseCount = 1))

        compose.onNodeWithTag("completed-summary").assertTextEquals("Total time 0:43 · 1 round · 1 exercise")
    }

    @Test
    fun anHourOrMoreShowsHours() {
        show(WorkoutSummary("Long", totalSeconds = 3663, roundCount = 10, exerciseCount = 80))

        compose.onNodeWithTag("completed-summary").assertTextEquals("Total time 1:01:03 · 10 rounds · 80 exercises")
    }

    @Test
    fun backToListCallsBack() {
        show(WorkoutSummary("Example", 263, 2, 5))

        compose.onNodeWithText(text(R.string.completed_back_to_list)).performClick()
        assertEquals(1, backClicks)
    }

    @Test
    fun theButtonIsLarge() {
        show(WorkoutSummary("Example", 263, 2, 5))
        compose.assertTouchTargetsAtLeast(72.dp)
    }
}
