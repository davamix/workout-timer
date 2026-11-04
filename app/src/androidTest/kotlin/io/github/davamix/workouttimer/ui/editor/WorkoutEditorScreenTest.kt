package io.github.davamix.workouttimer.ui.editor

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.repository.InMemoryWorkoutRepository
import io.github.davamix.workouttimer.ui.assertTouchTargetsAtLeast
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import io.github.davamix.workouttimer.ui.uiExampleWorkout
import io.github.davamix.workouttimer.ui.withoutIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The workout editor (P6 SPEC §4.3.2–§4.3.5) with an in-memory repository and fixed IDs: n1, n2, … */
@RunWith(AndroidJUnit4::class)
class WorkoutEditorScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var closes = 0
    private lateinit var repository: InMemoryWorkoutRepository
    private lateinit var viewModel: WorkoutEditorViewModel

    private fun open(
        workoutId: String?,
        vararg saved: Workout,
    ) {
        repository = InMemoryWorkoutRepository(saved.toList())
        var next = 0
        viewModel =
            WorkoutEditorViewModel(
                repository = repository,
                workoutId = workoutId,
                roundName = { text(R.string.editor_round_default_name, it) },
                newId = { "n${++next}" },
            )
        compose.setContent {
            WorkoutTimerTheme {
                WorkoutEditorScreen(workoutId = workoutId, onClose = { closes++ }, viewModel = viewModel)
            }
        }
        compose.waitUntil { viewModel.state !is EditorState.Loading }
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val form: WorkoutForm
        get() = (viewModel.state as EditorState.Editing).form

    private fun saved(): List<Workout> = runBlocking { repository.workouts.first() }

    /** Scrolls the form until the node tagged [tag] is composed, and returns it. */
    private fun node(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("editor-form").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    /** The node matching [matcher] inside the node tagged [tag], after scrolling to it. */
    private fun inside(
        tag: String,
        matcher: SemanticsMatcher,
    ): SemanticsNodeInteraction {
        val inTag = matcher and hasAnyAncestor(hasTestTag(tag))
        compose.onNodeWithTag("editor-form").performScrollToNode(inTag)
        return compose.onNode(inTag)
    }

    /** Every text a field shows: its value, label, placeholder, suffix and supporting text. */
    private fun texts(tag: String): List<String> {
        node(tag)
        return compose
            .onAllNodes(hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { node ->
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    .orEmpty()
                    .map { it.text } +
                    listOfNotNull(node.config.getOrNull(SemanticsProperties.EditableText)?.text)
            }
    }

    /** What is typed in the field tagged [tag]. */
    private fun value(tag: String): String =
        node(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    private fun type(
        tag: String,
        value: String,
    ) {
        node(tag).performTextReplacement(value)
    }

    private fun clickMore(
        tag: String,
        @StringRes item: Int,
    ) {
        inside(tag, hasContentDescription(text(R.string.editor_more))).performClick()
        compose.onNodeWithText(text(item)).performClick()
    }

    /** The system Back button. The keyboard is closed first, or Back would only close it. */
    private fun systemBack() {
        closeSoftKeyboard()
        pressBack()
    }

    private fun dialogButton(
        @StringRes label: Int,
    ) = compose.onNode(hasText(text(label)) and hasAnyAncestor(isDialog()))

    private fun assertTotal(total: String) {
        compose.onNodeWithTag("editor-total").assert(hasText(text(R.string.editor_total, total)))
    }

    // Creating

    @Test
    fun creatingTheExampleSavesItWithOwnAndLinkedValues() {
        open(workoutId = null)
        // The new form: workout n1, round n2 with exercise n3. Its defaults are already the example's.
        type("workout-name", "Example")
        type("round-name-n2", "Warm-up")
        type("round-rest-n2", "30")
        type("exercise-name-n3", "Jumping jacks")
        type("exercise-duration-n3", "30")
        inside("round-n2", hasText(text(R.string.editor_add_exercise))).performClick()
        type("exercise-name-n4", "Arm circles")
        type("exercise-duration-n4", "10")
        type("exercise-rest-n4", "5")
        inside("round-n2", hasText(text(R.string.editor_add_exercise))).performClick()
        type("exercise-name-n5", "High knees")
        compose.onNodeWithTag("editor-form").performScrollToNode(hasText(text(R.string.editor_add_round)))
        compose.onNodeWithText(text(R.string.editor_add_round)).performClick()
        type("round-name-n6", "Core")
        type("exercise-name-n7", "Plank")
        type("exercise-duration-n7", "60")
        type("exercise-rest-n7", "30")
        inside("round-n6", hasText(text(R.string.editor_add_exercise))).performClick()
        type("exercise-name-n8", "Crunches")

        assertTotal("4:23")
        compose.onNodeWithText(text(R.string.editor_save)).performClick()

        compose.waitUntil { closes == 1 }
        assertEquals(listOf(uiExampleWorkout.withoutIds()), saved().map { it.withoutIds() })
    }

    // Editing

    @Test
    fun changingADefaultChangesTheLinkedValuesOnly() {
        open(uiExampleWorkout.id, uiExampleWorkout)
        assertTotal("4:23")

        type("default-duration", "45")

        assertTrue(texts("exercise-duration-e-high-knees").contains("45"))
        assertTrue(texts("exercise-duration-e-crunches").contains("45"))
        assertEquals("30", value("exercise-duration-e-jumping-jacks"))
        assertTotal("4:33")
    }

    @Test
    fun clearingAnOwnValueShowsTheDefaultAgain() {
        open(uiExampleWorkout.id, uiExampleWorkout)

        inside("exercise-duration-e-jumping-jacks", hasContentDescription(text(R.string.editor_use_default))).performClick()

        assertEquals("", value("exercise-duration-e-jumping-jacks"))
        val shown = texts("exercise-duration-e-jumping-jacks")
        assertTrue(shown.toString(), shown.contains("40") && shown.contains(text(R.string.editor_uses_default)))
        assertTotal("4:33")
    }

    @Test
    fun unusedRestsAreMarkedAndTheMarkingFollowsAMove() {
        open(uiExampleWorkout.id, uiExampleWorkout)
        val notUsedExercise = text(R.string.editor_not_used_exercise)

        assertTrue(texts("exercise-rest-e-high-knees").contains(notUsedExercise))
        assertFalse(texts("exercise-rest-e-arm-circles").contains(notUsedExercise))
        assertTrue(texts("round-rest-r-core").contains(text(R.string.editor_not_used_round)))
        assertFalse(texts("round-rest-r-warm-up").contains(text(R.string.editor_not_used_round)))

        inside("exercise-e-high-knees", hasContentDescription(text(R.string.editor_move_up))).performClick()

        assertTrue(texts("exercise-rest-e-arm-circles").contains(notUsedExercise))
        assertFalse(texts("exercise-rest-e-high-knees").contains(notUsedExercise))
        // The own rest of Arm circles is kept while it's unused.
        assertEquals("5", value("exercise-rest-e-arm-circles"))
    }

    // Rounds and exercises

    @Test
    fun roundsAreAddedDuplicatedMovedAndRemoved() {
        open(uiExampleWorkout.id, uiExampleWorkout)

        compose.onNodeWithTag("editor-form").performScrollToNode(hasText(text(R.string.editor_add_round)))
        compose.onNodeWithText(text(R.string.editor_add_round)).performClick()
        assertEquals(listOf("r-warm-up", "r-core", "n1"), form.rounds.map { it.id })
        assertEquals(text(R.string.editor_round_default_name, 3), form.rounds.last().name)

        clickMore("round-header-r-warm-up", R.string.editor_duplicate)
        assertEquals(listOf("r-warm-up", "n3", "r-core", "n1"), form.rounds.map { it.id })
        assertEquals("Warm-up", form.rounds[1].name)

        inside("round-header-r-core", hasContentDescription(text(R.string.editor_move_down))).performClick()
        assertEquals(listOf("r-warm-up", "n3", "n1", "r-core"), form.rounds.map { it.id })

        clickMore("round-header-n1", R.string.editor_remove)
        assertEquals(listOf("r-warm-up", "n3", "r-core"), form.rounds.map { it.id })
    }

    @Test
    fun exercisesAreAddedDuplicatedMovedAndRemovedWithinTheirRound() {
        open(uiExampleWorkout.id, uiExampleWorkout)

        fun warmUp() =
            form.rounds
                .first()
                .exercises
                .map { it.id }

        inside("round-r-warm-up", hasText(text(R.string.editor_add_exercise))).performClick()
        assertEquals(listOf("e-jumping-jacks", "e-arm-circles", "e-high-knees", "n1"), warmUp())

        clickMore("exercise-e-arm-circles", R.string.editor_duplicate)
        assertEquals(listOf("e-jumping-jacks", "e-arm-circles", "n2", "e-high-knees", "n1"), warmUp())
        assertEquals(
            "Arm circles",
            form.rounds
                .first()
                .exercises[2]
                .name,
        )

        inside("exercise-e-jumping-jacks", hasContentDescription(text(R.string.editor_move_down))).performClick()
        assertEquals(listOf("e-arm-circles", "e-jumping-jacks", "n2", "e-high-knees", "n1"), warmUp())

        clickMore("exercise-n1", R.string.editor_remove)
        assertEquals(listOf("e-arm-circles", "e-jumping-jacks", "n2", "e-high-knees"), warmUp())
        assertEquals(
            listOf("e-plank", "e-crunches"),
            form.rounds
                .last()
                .exercises
                .map { it.id },
        )
    }

    // Validation

    @Test
    fun saveShowsTheErrorsAtTheirFieldsAndSavesNothing() {
        open(uiExampleWorkout.id, uiExampleWorkout)
        type("exercise-name-e-plank", "")
        type("exercise-duration-e-crunches", "0")
        type("exercise-rest-e-plank", "6000")

        compose.onNodeWithText(text(R.string.editor_save)).performClick()

        assertTrue(texts("exercise-name-e-plank").contains(text(R.string.editor_error_name)))
        assertTrue(texts("exercise-duration-e-crunches").contains(text(R.string.editor_error_range, 1, 5999)))
        assertTrue(texts("exercise-rest-e-plank").contains(text(R.string.editor_error_range, 0, 5999)))
        assertEquals(listOf(uiExampleWorkout), saved())
        assertEquals(0, closes)

        type("exercise-name-e-plank", "Plank")
        type("exercise-duration-e-crunches", "")
        type("exercise-rest-e-plank", "30")

        assertFalse(texts("exercise-name-e-plank").contains(text(R.string.editor_error_name)))
        assertFalse(texts("exercise-duration-e-crunches").contains(text(R.string.editor_error_range, 1, 5999)))
        assertFalse(texts("exercise-rest-e-plank").contains(text(R.string.editor_error_range, 0, 5999)))
    }

    @Test
    fun timeFieldsKeepOnlyDigits() {
        open(uiExampleWorkout.id, uiExampleWorkout)

        type("exercise-rest-e-arm-circles", "a-1.5 s")

        assertEquals("15", value("exercise-rest-e-arm-circles"))
    }

    // Leaving

    @Test
    fun backWithChangesAsksAndKeepEditingStays() {
        open(uiExampleWorkout.id, uiExampleWorkout)
        type("workout-name", "Renamed")

        systemBack()
        compose.onNodeWithText(text(R.string.editor_discard_title)).assertIsDisplayed()
        dialogButton(R.string.editor_discard_keep).performClick()

        compose.onNodeWithText(text(R.string.editor_discard_title)).assertDoesNotExist()
        assertEquals("Renamed", value("workout-name"))
        assertEquals(0, closes)
    }

    @Test
    fun discardLeavesWithoutSaving() {
        open(uiExampleWorkout.id, uiExampleWorkout)
        type("workout-name", "Renamed")

        compose.onNodeWithText(text(R.string.editor_cancel)).performClick()
        dialogButton(R.string.editor_discard_confirm).performClick()

        compose.waitUntil { closes == 1 }
        assertEquals(listOf(uiExampleWorkout), saved())
    }

    @Test
    fun backWithoutChangesLeavesAtOnce() {
        open(uiExampleWorkout.id, uiExampleWorkout)

        systemBack()

        compose.waitUntil { closes == 1 }
        compose.onNodeWithText(text(R.string.editor_discard_title)).assertDoesNotExist()
    }

    @Test
    fun anUnknownWorkoutSaysSoAndBackCloses() {
        open("missing")

        compose.onNodeWithText(text(R.string.editor_not_found)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_back)).performClick()

        compose.waitUntil { closes == 1 }
    }

    // Touch targets

    @Test
    fun touchTargetsAreLargeEnough() {
        open(uiExampleWorkout.id, uiExampleWorkout)

        compose.assertTouchTargetsAtLeast()

        type("workout-name", "Renamed")
        compose.onNodeWithText(text(R.string.editor_cancel)).performClick()
        compose.assertTouchTargetsAtLeast()
    }
}
