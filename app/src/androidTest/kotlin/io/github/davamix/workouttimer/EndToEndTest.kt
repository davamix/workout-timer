package io.github.davamix.workouttimer

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.davamix.workouttimer.audio.SpeakerStatus
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.di.AppContainer
import io.github.davamix.workouttimer.ui.editor.NewWorkoutDefaults
import io.github.davamix.workouttimer.ui.withoutIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole app wired together (P8 SPEC §4.3.4): it creates a workout in the editor, runs it from the list and completes
 * it, on the app's real storage, settings, navigation, clock and audio.
 *
 * The tablet speaks the cues during the test, which can't hear them; acceptance scenario A6 covers that. It doesn't
 * assume the list is empty: run with `am instrument`, the app's data is kept. The workout is deleted afterwards, and
 * the voice setting put back.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container: AppContainer
        get() = (compose.activity.application as WorkoutTimerApp).container

    private var voiceWasOn = true

    /** IDs are random here, so a new round or exercise is found by the editor tag that wasn't there before. */
    private val seenTags = mutableSetOf<String>()

    @Before
    fun setUp() {
        runBlocking {
            voiceWasOn =
                container.settingsRepository.settings
                    .first()
                    .voiceCuesEnabled
            container.settingsRepository.setVoiceCuesEnabled(true)
            deleteTestWorkouts()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            deleteTestWorkouts()
            container.settingsRepository.setVoiceCuesEnabled(voiceWasOn)
        }
    }

    @Test
    fun createRunAndCompleteAWorkout() {
        createTheWorkout()

        // The list shows what the editor saved.
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty() }
        val saved = savedWorkout()
        assertEquals(EXPECTED_WORKOUT.withoutIds(), saved.withoutIds())
        val row = "workout-${saved.id}"
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(row))
        compose.onNode(hasText(text(R.string.list_total, TOTAL)) and hasAnyAncestor(hasTestTag(row))).assertExists()

        // It runs by itself from Start to the completed screen.
        compose.onNode(hasText(text(R.string.list_start)) and hasAnyAncestor(hasTestTag(row))).performClick()
        waitForTitle("One", RUNNER_START_TIMEOUT_MILLIS)
        waitForTitle("Two", NEXT_PHASE_TIMEOUT_MILLIS)
        waitForTitle("Three", NEXT_PHASE_TIMEOUT_MILLIS)
        compose.waitUntil(LAST_EXERCISE_TIMEOUT_MILLIS) {
            compose.onAllNodesWithTag("completed-title").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("completed-title").assertTextEquals(text(R.string.completed_title))
        compose.onNodeWithTag("completed-summary").assertTextEquals(expectedSummary())
        assertEquals(SpeakerStatus.READY, container.speaker.status.value)

        // Back on the list, the workout is still there.
        click(R.string.completed_back_to_list)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("completed-title").fetchSemanticsNodes().isEmpty() }
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(row))
        compose.onNodeWithTag(row).assertExists()
    }

    /** Types [EXPECTED_WORKOUT] into a new workout's form, and saves it. */
    private fun createTheWorkout() {
        click(R.string.list_new_workout)

        // A new workout has one round holding one unnamed exercise.
        val roundA = newId("round-name-")
        type("workout-name", WORKOUT_NAME)
        type("default-exercise-rest", "0")
        type("default-round-rest", "0")
        type("round-name-$roundA", "Round A")
        type("round-rest-$roundA", "1")
        val one = newId("exercise-name-")
        type("exercise-name-$one", "One")
        type("exercise-duration-$one", "3")
        type("exercise-rest-$one", "1")

        addExerciseTo(roundA)
        val two = newId("exercise-name-")
        type("exercise-name-$two", "Two")
        type("exercise-duration-$two", "3")

        clickInForm(hasText(text(R.string.editor_add_round)))
        val roundB = newId("round-name-")
        type("round-name-$roundB", "Round B")
        val three = newId("exercise-name-")
        type("exercise-name-$three", "Three")
        type("exercise-duration-$three", "11")

        compose.onNodeWithTag("editor-total").assert(hasText(text(R.string.editor_total, TOTAL)))
        closeSoftKeyboard()
        click(R.string.editor_save)
    }

    private fun form() = compose.onNodeWithTag("editor-form")

    private fun type(
        tag: String,
        value: String,
    ) {
        form().performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performTextReplacement(value)
    }

    private fun addExerciseTo(roundId: String) {
        clickInForm(hasText(text(R.string.editor_add_exercise)) and hasAnyAncestor(hasTestTag("round-$roundId")))
    }

    /**
     * Scrolls to the node and clicks it, with the keyboard closed first. MainActivity resizes for the keyboard
     * (adjustResize), and focus moving from a name to a number swaps the letter keyboard for the shorter number pad, a
     * moment later. If that lands between the scroll and the click, the form moves under the click, or the node has
     * empty bounds and the click silently lands at the screen's corner.
     */
    private fun clickInForm(matcher: SemanticsMatcher) {
        closeSoftKeyboard()
        form().performScrollToNode(matcher)
        compose.onNode(matcher).performClick()
    }

    /** The ID in the first editor tag `<prefix><id>` that hasn't been seen yet. */
    private fun newId(prefix: String): String {
        val tags = {
            compose
                .onAllNodes(hasTagStartingWith(prefix))
                .fetchSemanticsNodes()
                .map { it.config[SemanticsProperties.TestTag] }
                .filterNot { it in seenTags }
        }
        compose.waitUntil(5_000) { tags().isNotEmpty() }
        val tag = tags().first()
        seenTags += tag
        return tag.removePrefix(prefix)
    }

    private fun hasTagStartingWith(prefix: String) =
        SemanticsMatcher("test tag starts with '$prefix'") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        }

    private fun waitForTitle(
        title: String,
        timeoutMillis: Long,
    ) {
        compose.waitUntil(timeoutMillis) {
            compose
                .onAllNodesWithTag("runner-title")
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.Text)
                ?.joinToString { it.text } == title
        }
    }

    private fun expectedSummary(): String {
        val resources = compose.activity.resources
        return text(
            R.string.completed_summary,
            TOTAL,
            resources.getQuantityString(R.plurals.completed_rounds, 2, 2),
            resources.getQuantityString(R.plurals.completed_exercises, 3, 3),
        )
    }

    private fun savedWorkout(): Workout = runBlocking { testWorkouts().single() }

    private suspend fun testWorkouts(): List<Workout> =
        container.workoutRepository.workouts
            .first()
            .filter { it.name == WORKOUT_NAME }

    private suspend fun deleteTestWorkouts() {
        testWorkouts().forEach { container.workoutRepository.delete(it.id) }
    }

    private fun click(
        @StringRes id: Int,
    ) {
        compose.onNodeWithText(text(id)).performClick()
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = compose.activity.getString(id, *args)

    private companion object {
        const val WORKOUT_NAME = "End-to-end test"

        /** 3 s get ready + 17 s exercises + 2 s rests. */
        const val TOTAL = "0:22"

        /** Preparing the audio takes up to 10 s after a cold start (P5), then "Get ready in" and the 3 s get ready. */
        const val RUNNER_START_TIMEOUT_MILLIS = 20_000L

        /** A 3 s exercise and a 1 s rest, with room to spare. */
        const val NEXT_PHASE_TIMEOUT_MILLIS = 10_000L

        /** The 11 s exercise, with room to spare. */
        const val LAST_EXERCISE_TIMEOUT_MILLIS = 20_000L

        val EXPECTED_WORKOUT =
            Workout(
                id = "",
                name = WORKOUT_NAME,
                defaultExerciseDurationSeconds = NewWorkoutDefaults.EXERCISE_DURATION_SECONDS,
                defaultExerciseRestSeconds = 0,
                defaultRoundRestSeconds = 0,
                rounds =
                    listOf(
                        Round(
                            id = "",
                            name = "Round A",
                            restSeconds = 1,
                            exercises =
                                listOf(
                                    Exercise("", "One", durationSeconds = 3, restSeconds = 1),
                                    Exercise("", "Two", durationSeconds = 3),
                                ),
                        ),
                        Round(
                            id = "",
                            name = "Round B",
                            exercises = listOf(Exercise("", "Three", durationSeconds = 11)),
                        ),
                    ),
            )
    }
}
