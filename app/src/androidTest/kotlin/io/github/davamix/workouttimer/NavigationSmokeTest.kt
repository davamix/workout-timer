package io.github.davamix.workouttimer

import android.content.pm.ActivityInfo
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that every screen is reachable from the workout list, and that the activity is landscape only (NF-8).
 *
 * It runs on the app's real storage, so it seeds one workout with a fixed ID to start, and deletes it afterwards. It
 * doesn't assume the list is otherwise empty: run with `am instrument`, the app's data is kept.
 */
@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container: AppContainer
        get() = (compose.activity.application as WorkoutTimerApp).container

    private val seeded =
        Workout(
            id = SEEDED_ID,
            name = "Smoke test",
            defaultExerciseDurationSeconds = 40,
            defaultExerciseRestSeconds = 15,
            defaultRoundRestSeconds = 60,
            rounds = listOf(Round("r-$SEEDED_ID", "Round 1", exercises = listOf(Exercise("e-$SEEDED_ID", "Squats")))),
        )

    @Before
    fun seed() {
        runBlocking { container.workoutRepository.save(seeded) }
    }

    @After
    fun clean() {
        runBlocking { container.workoutRepository.delete(SEEDED_ID) }
    }

    @Test
    fun startsOnWorkoutList() {
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun editorOpensAndReturnsWithBackButton() {
        click(R.string.list_new_workout)
        assertDisplayed(R.string.editor_title_new)

        click(R.string.action_back)
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun settingsOpenAndReturnWithSystemBack() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.list_settings)).performClick()
        assertDisplayed(R.string.settings_title)

        pressBack()
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun runnerLeadsToCompletedAndBackToList() {
        startSeededWorkout()
        assertDisplayed(R.string.runner_title)

        click(R.string.runner_finish)
        assertDisplayed(R.string.completed_title)

        click(R.string.completed_back_to_list)
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun completedBackSkipsTheRunner() {
        startSeededWorkout()
        click(R.string.runner_finish)

        pressBack()
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun activityIsLandscapeOnly() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, compose.activity.requestedOrientation)
    }

    /** Presses Start on the seeded workout, once the list shows it. */
    private fun startSeededWorkout() {
        val start = hasText(compose.activity.getString(R.string.list_start)) and hasAnyAncestor(hasTestTag("workout-$SEEDED_ID"))
        compose.waitUntil { compose.onAllNodes(start).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(start).performClick()
    }

    private fun click(
        @StringRes text: Int,
    ) {
        compose.onNodeWithText(compose.activity.getString(text)).performClick()
    }

    private fun assertDisplayed(
        @StringRes text: Int,
    ) {
        compose.onNodeWithText(compose.activity.getString(text)).assertExists()
    }

    private companion object {
        const val SEEDED_ID = "smoke-test-workout"
    }
}
