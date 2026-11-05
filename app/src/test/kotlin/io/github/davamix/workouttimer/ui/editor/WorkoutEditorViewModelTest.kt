package io.github.davamix.workouttimer.ui.editor

import io.github.davamix.workouttimer.core.model.TimeField
import io.github.davamix.workouttimer.data.exampleWorkout
import io.github.davamix.workouttimer.ui.MainDispatcherExtension
import io.github.davamix.workouttimer.ui.TestWorkoutRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WorkoutEditorViewModelTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    /** IDs n1, n2, … in the order they're asked for. */
    private fun ids(): () -> String {
        var next = 0
        return { "n${++next}" }
    }

    private fun editor(
        repository: TestWorkoutRepository,
        workoutId: String?,
    ) = WorkoutEditorViewModel(repository, workoutId, roundName = { "Round $it" }, newId = ids())

    private val WorkoutEditorViewModel.editing: EditorState.Editing
        get() = state as EditorState.Editing

    // Opening

    @Test
    fun `a new workout opens with the new-workout form, unchanged and without errors`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(), workoutId = null)

            assertEquals(
                EditorState.Editing(
                    form = newWorkoutForm("Round 1", ids()),
                    isNew = true,
                    totalSeconds = 43,
                    errors = EditorErrors.NONE,
                    hasChanges = false,
                ),
                viewModel.state,
            )
        }

    @Test
    fun `a saved workout is loading, then opens as it is`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout), loaded = false)
            val viewModel = editor(repository, exampleWorkout.id)

            assertEquals(EditorState.Loading, viewModel.state)

            repository.finishLoading()

            assertEquals(
                EditorState.Editing(
                    form = exampleWorkout.toForm(),
                    isNew = false,
                    totalSeconds = 263,
                    errors = EditorErrors.NONE,
                    hasChanges = false,
                ),
                viewModel.state,
            )
        }

    @Test
    fun `an ID with no saved workout gives not found`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), "missing")

            assertEquals(EditorState.NotFound, viewModel.state)
        }

    // Editing

    @Test
    fun `an edit updates the form and the total`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)

            viewModel.edit { it.withDefault(DefaultField.EXERCISE_DURATION, "45") }

            assertEquals("45", viewModel.editing.form.defaultExerciseDuration)
            assertEquals(273, viewModel.editing.totalSeconds)
        }

    @Test
    fun `an edit counts as a change, and putting the value back does not`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)

            viewModel.edit { it.withExerciseDuration("e-plank", "45") }
            assertTrue(viewModel.editing.hasChanges)

            viewModel.edit { it.withExerciseDuration("e-plank", "60") }
            assertFalse(viewModel.editing.hasChanges)
        }

    @Test
    fun `text that maps to the same workout is not a change`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)

            viewModel.edit { it.withExerciseDuration("e-plank", "060").withName(" Example ") }

            assertFalse(viewModel.editing.hasChanges)
        }

    @Test
    fun `a new round is called Round n, after the number of rounds it makes`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)

            val id = viewModel.addRound()

            assertEquals("n1", id)
            assertEquals(
                RoundForm("n1", "Round 3", "", listOf(ExerciseForm("n2", "", "", ""))),
                viewModel.editing.form.rounds
                    .last(),
            )
            assertTrue(viewModel.editing.hasChanges)
        }

    @Test
    fun `adding and duplicating return the ID of what they added`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)

            assertEquals("n1", viewModel.addExercise("r-core"))
            assertEquals("n2", viewModel.duplicateRound("r-warm-up"))
            assertEquals("n6", viewModel.duplicateExercise("e-plank"))
            assertNull(viewModel.addExercise("missing"))
            assertEquals(
                listOf("r-warm-up", "n2", "r-core"),
                viewModel.editing.form.rounds
                    .map { it.id },
            )
        }

    // Validation and saving

    @Test
    fun `no errors show before the first Save attempt`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(), workoutId = null)

            viewModel.edit { it.withDefault(DefaultField.EXERCISE_DURATION, "0") }

            assertEquals(EditorErrors.NONE, viewModel.editing.errors)
        }

    @Test
    fun `Save on an invalid form saves nothing, shows the errors and asks to check them`() =
        runTest {
            val repository = TestWorkoutRepository()
            val viewModel = editor(repository, workoutId = null)

            viewModel.save()

            assertEquals(0, repository.saveCalls)
            assertEquals(
                EditorErrors(workoutNameMissing = true, exerciseNameMissing = setOf("n3")),
                viewModel.editing.errors,
            )
            assertEquals(EditorMessage.CHECK_FIELDS, viewModel.editing.message)
        }

    @Test
    fun `after a Save attempt, the errors follow every edit`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(), workoutId = null)
            viewModel.save()

            viewModel.edit { it.withName("Legs") }
            assertEquals(EditorErrors(exerciseNameMissing = setOf("n3")), viewModel.editing.errors)

            viewModel.edit { it.withExerciseDuration("n3", "0") }
            assertEquals(
                EditorErrors(exerciseNameMissing = setOf("n3"), timeOutOfRange = mapOf(TimeField.ExerciseDuration("n3") to 1..5999)),
                viewModel.editing.errors,
            )

            viewModel.edit { it.withExerciseName("n3", "Squats").withExerciseDuration("n3", "") }
            assertEquals(EditorErrors.NONE, viewModel.editing.errors)
        }

    @Test
    fun `Save on a valid new workout saves it and closes the editor`() =
        runTest {
            val repository = TestWorkoutRepository()
            val viewModel = editor(repository, workoutId = null)
            viewModel.edit { it.withName(" Legs ").withExerciseName("n3", "Squats") }
            val expected = viewModel.editing.form.toWorkout()

            viewModel.save()

            assertEquals(EditorState.Closed, viewModel.state)
            assertEquals(listOf(expected), repository.all())
            assertEquals("Legs", expected.name)
            assertEquals("n1", expected.id)
        }

    @Test
    fun `Save on an edited workout replaces the saved one`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout))
            val viewModel = editor(repository, exampleWorkout.id)
            viewModel.edit { it.withDefault(DefaultField.EXERCISE_DURATION, "45") }

            viewModel.save()

            assertEquals(EditorState.Closed, viewModel.state)
            assertEquals(listOf(exampleWorkout.copy(defaultExerciseDurationSeconds = 45)), repository.all())
        }

    @Test
    fun `a failed save keeps the form and says so`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout)).apply { failWrites = true }
            val viewModel = editor(repository, exampleWorkout.id)
            viewModel.edit { it.withName("Renamed") }
            val form = viewModel.editing.form

            viewModel.save()

            assertEquals(form, viewModel.editing.form)
            assertEquals(EditorMessage.SAVE_FAILED, viewModel.editing.message)
            assertFalse(viewModel.editing.saving)
            assertTrue(viewModel.editing.hasChanges)
            assertEquals(listOf(exampleWorkout), repository.all())

            viewModel.messageShown()
            assertNull(viewModel.editing.message)
        }

    // Leaving

    @Test
    fun `leaving with no changes closes at once`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)

            viewModel.requestClose()

            assertEquals(EditorState.Closed, viewModel.state)
        }

    @Test
    fun `leaving an untouched new workout closes at once`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(), workoutId = null)

            viewModel.requestClose()

            assertEquals(EditorState.Closed, viewModel.state)
        }

    @Test
    fun `leaving with changes asks, and Keep editing stays`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(listOf(exampleWorkout)), exampleWorkout.id)
            viewModel.edit { it.withName("Renamed") }

            viewModel.requestClose()
            assertTrue(viewModel.editing.confirmingDiscard)

            viewModel.cancelDiscard()
            assertFalse(viewModel.editing.confirmingDiscard)
            assertEquals("Renamed", viewModel.editing.form.name)
        }

    @Test
    fun `Discard closes and saves nothing`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout))
            val viewModel = editor(repository, exampleWorkout.id)
            viewModel.edit { it.withName("Renamed") }

            viewModel.requestClose()
            viewModel.confirmDiscard()

            assertEquals(EditorState.Closed, viewModel.state)
            assertEquals(0, repository.saveCalls)
            assertEquals(listOf(exampleWorkout), repository.all())
        }

    @Test
    fun `leaving the not-found state closes`() =
        runTest {
            val viewModel = editor(TestWorkoutRepository(), "missing")

            viewModel.requestClose()

            assertEquals(EditorState.Closed, viewModel.state)
        }
}
