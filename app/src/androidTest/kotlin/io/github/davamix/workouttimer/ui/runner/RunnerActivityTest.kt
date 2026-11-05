package io.github.davamix.workouttimer.ui.runner

import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.davamix.workouttimer.MainActivity
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The runner inside `MainActivity`, on the app's real storage (P7 SPEC §4.3.4, §4.3.6): recreation (NF-3), the
 * activity stopping (EX-10), and keeping the screen on (NF-2).
 *
 * It seeds one workout with a fixed ID, a 60 s exercise, and turns voice cues off for its run, so get ready counts down
 * at once and nothing is spoken. Both are put back afterwards.
 */
@RunWith(AndroidJUnit4::class)
class RunnerActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container: AppContainer
        get() = (compose.activity.application as WorkoutTimerApp).container

    private var voiceWasOn = true

    private val seeded =
        Workout(
            id = SEEDED_ID,
            name = "Runner activity test",
            defaultExerciseDurationSeconds = 40,
            defaultExerciseRestSeconds = 15,
            defaultRoundRestSeconds = 60,
            rounds = listOf(Round("r-$SEEDED_ID", "Round 1", exercises = listOf(Exercise("e-$SEEDED_ID", "Squats", 60)))),
        )

    @Before
    fun seed() {
        runBlocking {
            voiceWasOn =
                container.settingsRepository.settings
                    .first()
                    .voiceCuesEnabled
            container.settingsRepository.setVoiceCuesEnabled(false)
            container.workoutRepository.save(seeded)
        }
    }

    @After
    fun clean() {
        runBlocking {
            container.workoutRepository.delete(SEEDED_ID)
            container.settingsRepository.setVoiceCuesEnabled(voiceWasOn)
        }
    }

    private fun tag(tag: String) = compose.onNodeWithTag(tag)

    private fun waitForTag(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        compose.waitUntil(timeoutMillis) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun textOf(tag: String): String? =
        compose
            .onAllNodesWithTag(tag)
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.config
            ?.getOrNull(SemanticsProperties.Text)
            ?.joinToString { it.text }

    /** The time shown, in seconds: "00:57" gives 57. */
    private fun secondsShown(): Int {
        val (minutes, seconds) = checkNotNull(textOf("runner-time")) { "No time shown" }.split(":").map { it.toInt() }
        return minutes * 60 + seconds
    }

    private fun keepsScreenOn(): Boolean =
        compose.runOnUiThread {
            compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        }

    /** Presses Start on the seeded workout, then waits until its exercise runs. */
    private fun startTheExercise() {
        val start = hasText(compose.activity.getString(R.string.list_start)) and hasAnyAncestor(hasTestTag("workout-$SEEDED_ID"))
        compose.waitUntil(10_000) { compose.onAllNodes(start).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(start).performClick()
        compose.waitUntil(RUNNER_START_TIMEOUT_MILLIS) { textOf("runner-title") == "Squats" }
    }

    private fun pauseAndOpenTheStopDialog() {
        tag("runner-button-pause").performClick()
        waitForTag("runner-paused")
        tag("runner-button-stop").performClick()
        waitForTag("runner-stop-confirm")
    }

    @Test
    fun recreationKeepsTheWorkoutRunning() {
        startTheExercise()
        val before = secondsShown()

        compose.activityRule.scenario.recreate()

        waitForTag("runner-button-pause")
        tag("runner-paused").assertDoesNotExist()
        tag("runner-title").assertTextEquals("Squats")
        assertTrue("Reset to ${secondsShown()} s from $before s", secondsShown() <= before)
        compose.waitUntil(5_000) { secondsShown() < before }
    }

    @Test
    fun recreationKeepsTheStopDialogOpen() {
        startTheExercise()
        pauseAndOpenTheStopDialog()

        compose.activityRule.scenario.recreate()

        waitForTag("runner-stop-confirm")
        tag("runner-paused").assertExists()
        tag("runner-stop-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("runner-time").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun stoppingTheActivityPausesTheWorkout() {
        startTheExercise()

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        waitForTag("runner-paused")
        tag("runner-button-resume").assertExists()
    }

    @Test
    fun theScreenStaysOnOnlyWhileTheRunnerIsOpen() {
        assertFalse(keepsScreenOn())

        startTheExercise()
        assertTrue(keepsScreenOn())

        pauseAndOpenTheStopDialog()
        tag("runner-stop-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("runner-time").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(5_000) { !keepsScreenOn() }
    }

    private companion object {
        const val SEEDED_ID = "runner-activity-test"

        /** Get ready (3 s), after preparing the audio, which takes up to 10 s after a cold start (P5). */
        const val RUNNER_START_TIMEOUT_MILLIS = 20_000L
    }
}
