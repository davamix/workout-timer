package io.github.davamix.workouttimer.ui.list

import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.timeline.estimatedTotalSeconds
import io.github.davamix.workouttimer.data.exampleWorkout
import io.github.davamix.workouttimer.data.exercise
import io.github.davamix.workouttimer.data.round
import io.github.davamix.workouttimer.data.singleExerciseWorkout
import io.github.davamix.workouttimer.data.workout
import io.github.davamix.workouttimer.ui.MainDispatcherExtension
import io.github.davamix.workouttimer.ui.TestWorkoutRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WorkoutListViewModelTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private fun item(workout: Workout) = WorkoutListItem(workout.id, workout.name, workout.estimatedTotalSeconds())

    /** Every ID in [workout]: the workout's, its rounds' and its exercises'. */
    private fun idsOf(workout: Workout) =
        listOf(workout.id) + workout.rounds.flatMap { round -> listOf(round.id) + round.exercises.map { it.id } }

    /** [workout] with every ID blanked, to compare structures apart from IDs. */
    private fun withoutIds(workout: Workout) =
        workout.copy(
            id = "",
            rounds = workout.rounds.map { round -> round.copy(id = "", exercises = round.exercises.map { it.copy(id = "") }) },
        )

    @Test
    fun `it is loading until the repository has loaded, then shows the workouts`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout), loaded = false)
            val viewModel = WorkoutListViewModel(repository)

            assertEquals(WorkoutListState(loading = true), viewModel.state.value)

            repository.finishLoading()

            assertEquals(WorkoutListState(loading = false, workouts = listOf(item(exampleWorkout))), viewModel.state.value)
        }

    @Test
    fun `an empty repository gives an empty list, not loading`() =
        runTest {
            val viewModel = WorkoutListViewModel(TestWorkoutRepository())

            assertEquals(WorkoutListState(loading = false), viewModel.state.value)
        }

    @Test
    fun `workouts are sorted by name ignoring case, then by ID`() =
        runTest {
            val beta = workout(round("R", exercise("E")), name = "beta", id = "b")
            val upperAlpha = workout(round("R", exercise("E")), name = "Alpha", id = "a2")
            val lowerAlpha = workout(round("R", exercise("E")), name = "alpha", id = "a1")
            val viewModel = WorkoutListViewModel(TestWorkoutRepository(listOf(beta, upperAlpha, lowerAlpha)))

            assertEquals(
                listOf("a1", "a2", "b"),
                viewModel.state.value.workouts
                    .map { it.id },
            )
        }

    @Test
    fun `each workout shows its total from Start to completion`() =
        runTest {
            val viewModel = WorkoutListViewModel(TestWorkoutRepository(listOf(exampleWorkout, singleExerciseWorkout)))

            val totals =
                viewModel.state.value.workouts
                    .associate { it.name to it.totalSeconds }

            // The example is 4:23; the single exercise is get ready 3 s plus the default 40 s.
            assertEquals(mapOf("Example" to 263, "Single" to 43), totals)
        }

    @Test
    fun `the list follows changes saved elsewhere, such as by the editor`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout))
            val viewModel = WorkoutListViewModel(repository)

            repository.save(exampleWorkout.copy(name = "Renamed"))
            repository.save(singleExerciseWorkout)

            assertEquals(
                listOf("Renamed", "Single"),
                viewModel.state.value.workouts
                    .map { it.name },
            )
        }

    @Test
    fun `duplicate saves a copy with the given name, new IDs and the same values`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout))
            val viewModel = WorkoutListViewModel(repository)

            viewModel.duplicate(exampleWorkout.id, "Example (copy)")

            val copy = repository.all().single { it.id != exampleWorkout.id }
            assertEquals("Example (copy)", copy.name)
            assertEquals(withoutIds(exampleWorkout.copy(name = "Example (copy)")), withoutIds(copy))
            assertTrue(idsOf(copy).none { it in idsOf(exampleWorkout) })
            assertEquals(
                listOf("Example", "Example (copy)"),
                viewModel.state.value.workouts
                    .map { it.name },
            )
            assertEquals(exampleWorkout, repository.get(exampleWorkout.id))
        }

    @Test
    fun `delete asks first, and cancelling keeps the workout`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout))
            val viewModel = WorkoutListViewModel(repository)

            viewModel.requestDelete(exampleWorkout.id)

            assertEquals(item(exampleWorkout), viewModel.state.value.pendingDelete)
            assertEquals(listOf(exampleWorkout), repository.all())

            viewModel.cancelDelete()

            assertNull(viewModel.state.value.pendingDelete)
            assertEquals(listOf(exampleWorkout), repository.all())
        }

    @Test
    fun `confirming the delete deletes the workout and closes the confirmation`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout, singleExerciseWorkout))
            val viewModel = WorkoutListViewModel(repository)

            viewModel.requestDelete(exampleWorkout.id)
            viewModel.confirmDelete()

            assertEquals(listOf(singleExerciseWorkout), repository.all())
            assertEquals(WorkoutListState(loading = false, workouts = listOf(item(singleExerciseWorkout))), viewModel.state.value)
        }

    @Test
    fun `confirming with no confirmation open does nothing`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout))
            val viewModel = WorkoutListViewModel(repository)

            viewModel.confirmDelete()

            assertEquals(listOf(exampleWorkout), repository.all())
        }

    @Test
    fun `a failed duplicate shows its message and changes nothing`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout)).apply { failWrites = true }
            val viewModel = WorkoutListViewModel(repository)

            viewModel.duplicate(exampleWorkout.id, "Example (copy)")

            assertEquals(ListMessage.DUPLICATE_FAILED, viewModel.state.value.message)
            assertEquals(listOf(exampleWorkout), repository.all())
            assertEquals(listOf(item(exampleWorkout)), viewModel.state.value.workouts)
        }

    @Test
    fun `a failed delete shows its message and keeps the workout`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout)).apply { failWrites = true }
            val viewModel = WorkoutListViewModel(repository)

            viewModel.requestDelete(exampleWorkout.id)
            viewModel.confirmDelete()

            assertEquals(ListMessage.DELETE_FAILED, viewModel.state.value.message)
            assertNull(viewModel.state.value.pendingDelete)
            assertEquals(listOf(item(exampleWorkout)), viewModel.state.value.workouts)
        }

    @Test
    fun `a message is cleared once shown`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout)).apply { failWrites = true }
            val viewModel = WorkoutListViewModel(repository)
            viewModel.duplicate(exampleWorkout.id, "Copy")

            viewModel.messageShown()

            assertNull(viewModel.state.value.message)
        }
}
