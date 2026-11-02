package io.github.davamix.workouttimer

import android.content.pm.ActivityInfo
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the P1 skeleton: every placeholder screen is reachable, and the activity is landscape only (NF-8). */
@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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
        click(R.string.list_settings)
        assertDisplayed(R.string.settings_title)

        pressBack()
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun runnerLeadsToCompletedAndBackToList() {
        click(R.string.list_run_sample)
        assertDisplayed(R.string.runner_title)

        click(R.string.runner_finish)
        assertDisplayed(R.string.completed_title)

        click(R.string.completed_back_to_list)
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun completedBackSkipsTheRunner() {
        click(R.string.list_run_sample)
        click(R.string.runner_finish)

        pressBack()
        assertDisplayed(R.string.list_title)
    }

    @Test
    fun activityIsLandscapeOnly() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, compose.activity.requestedOrientation)
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
}
