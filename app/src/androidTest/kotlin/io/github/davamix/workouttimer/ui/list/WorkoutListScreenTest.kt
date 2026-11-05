package io.github.davamix.workouttimer.ui.list

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.repository.InMemoryWorkoutRepository
import io.github.davamix.workouttimer.ui.assertTouchTargetsAtLeast
import io.github.davamix.workouttimer.ui.simpleWorkout
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import io.github.davamix.workouttimer.ui.uiExampleWorkout
import io.github.davamix.workouttimer.ui.withoutIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The workout list screen (P6 SPEC §4.3.1) with an in-memory repository. */
@RunWith(AndroidJUnit4::class)
class WorkoutListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val newWorkoutClicks = mutableListOf<Unit>()
    private val settingsClicks = mutableListOf<Unit>()
    private val edited = mutableListOf<String>()
    private val started = mutableListOf<String>()

    private fun show(vararg workouts: Workout): InMemoryWorkoutRepository {
        val repository = InMemoryWorkoutRepository(workouts.toList())
        val viewModel = WorkoutListViewModel(repository)
        compose.setContent {
            WorkoutTimerTheme {
                WorkoutListScreen(
                    onNewWorkout = { newWorkoutClicks += Unit },
                    onEditWorkout = { edited += it },
                    onStartWorkout = { started += it },
                    onOpenSettings = { settingsClicks += Unit },
                    viewModel = viewModel,
                )
            }
        }
        return repository
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    /** The button labelled [label] in the row of the workout [workoutId]. */
    private fun rowButton(
        workoutId: String,
        @StringRes label: Int,
    ) = compose.onNode(hasText(text(label)) and hasAnyAncestor(hasTestTag("workout-$workoutId")))

    private fun dialogButton(
        @StringRes label: Int,
    ) = compose.onNode(hasText(text(label)) and hasAnyAncestor(isDialog()))

    private val isWorkoutRow =
        SemanticsMatcher("is a workout row") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("workout-") == true }

    private fun rowIds() =
        compose
            .onAllNodes(isWorkoutRow)
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag].removePrefix("workout-") }

    private fun waitForRows(count: Int) = compose.waitUntil { compose.onAllNodes(isWorkoutRow).fetchSemanticsNodes().size == count }

    @Test
    fun rowsAreSortedByNameAndShowTheirTotals() {
        show(simpleWorkout("legs", "w-legs"), uiExampleWorkout, simpleWorkout("Arms", "w-arms"))
        waitForRows(3)

        assertEquals(listOf("w-arms", "w-example", "w-legs"), rowIds())
        compose
            .onNode(hasText(text(R.string.list_total, "4:23")) and hasAnyAncestor(hasTestTag("workout-w-example")))
            .assertIsDisplayed()
        compose
            .onNode(hasText(text(R.string.list_total, "0:43")) and hasAnyAncestor(hasTestTag("workout-w-arms")))
            .assertIsDisplayed()
    }

    @Test
    fun anEmptyListShowsTheMessage() {
        show()

        compose.onNodeWithText(text(R.string.list_empty)).assertIsDisplayed()
    }

    @Test
    fun startAndEditPassTheWorkoutId() {
        show(uiExampleWorkout, simpleWorkout("Arms", "w-arms"))
        waitForRows(2)

        rowButton("w-example", R.string.list_start).performClick()
        rowButton("w-arms", R.string.list_edit).performClick()

        assertEquals(listOf("w-example"), started)
        assertEquals(listOf("w-arms"), edited)
    }

    @Test
    fun newWorkoutAndSettingsCallBack() {
        show(uiExampleWorkout)

        compose.onNodeWithText(text(R.string.list_new_workout)).performClick()
        compose.onNodeWithContentDescription(text(R.string.list_settings)).performClick()

        assertEquals(1, newWorkoutClicks.size)
        assertEquals(1, settingsClicks.size)
    }

    @Test
    fun duplicateAddsACopyToTheList() {
        val repository = show(uiExampleWorkout)
        waitForRows(1)

        rowButton("w-example", R.string.list_duplicate).performClick()

        waitForRows(2)
        compose.onNodeWithText(text(R.string.list_copy_name, "Example")).assertIsDisplayed()
        val copy = runBlocking { repository.workouts.first() }.single { it.id != uiExampleWorkout.id }
        assertEquals(uiExampleWorkout.copy(name = "Example (copy)").withoutIds(), copy.withoutIds())
    }

    @Test
    fun deleteAsksFirstAndCancelKeepsTheWorkout() {
        val repository = show(uiExampleWorkout)
        waitForRows(1)

        rowButton("w-example", R.string.list_delete).performClick()

        compose.onNodeWithText(text(R.string.list_delete_title, "Example")).assertIsDisplayed()
        dialogButton(R.string.list_delete_cancel).performClick()

        compose.onNodeWithText(text(R.string.list_delete_title, "Example")).assertDoesNotExist()
        assertEquals(listOf("w-example"), rowIds())
        assertEquals(listOf(uiExampleWorkout), runBlocking { repository.workouts.first() })
    }

    @Test
    fun confirmingTheDeleteRemovesTheWorkout() {
        val repository = show(uiExampleWorkout, simpleWorkout("Arms", "w-arms"))
        waitForRows(2)

        rowButton("w-example", R.string.list_delete).performClick()
        dialogButton(R.string.list_delete_confirm).performClick()

        waitForRows(1)
        assertEquals(listOf("w-arms"), rowIds())
        assertEquals(listOf("w-arms"), runBlocking { repository.workouts.first() }.map { it.id })
    }

    @Test
    fun touchTargetsAreLargeEnough() {
        show(uiExampleWorkout)
        waitForRows(1)

        compose.assertTouchTargetsAtLeast()

        rowButton("w-example", R.string.list_delete).performClick()
        compose.assertTouchTargetsAtLeast()
    }
}
