package io.github.davamix.workouttimer.core.repository

import io.github.davamix.workouttimer.core.exampleWorkout
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InMemoryWorkoutRepositoryTest {
    private val legs = workout(round("R", exercise("Squats")), name = "Legs")
    private val arms = workout(round("R", exercise("Push-ups")), name = "Arms")

    /** The order of [WorkoutRepository.workouts] isn't specified, so lists are compared as sets. */
    private suspend fun WorkoutRepository.saved(): Set<Workout> = workouts.first().toSet()

    @Test
    fun `a new repository is empty`() =
        runTest {
            val repository = InMemoryWorkoutRepository()

            assertEquals(emptySet<Workout>(), repository.saved())
            assertNull(repository.get(legs.id))
        }

    @Test
    fun `a repository starts with its initial workouts`() =
        runTest {
            val repository = InMemoryWorkoutRepository(listOf(legs, arms))

            assertEquals(setOf(legs, arms), repository.saved())
            assertEquals(arms, repository.get(arms.id))
        }

    @Test
    fun `a saved workout can be read back`() =
        runTest {
            val repository = InMemoryWorkoutRepository()

            repository.save(exampleWorkout)

            assertEquals(exampleWorkout, repository.get(exampleWorkout.id))
            assertEquals(setOf(exampleWorkout), repository.saved())
        }

    @Test
    fun `saving a workout with a known ID replaces it`() =
        runTest {
            val repository = InMemoryWorkoutRepository(listOf(legs, arms))
            val renamed = legs.copy(name = "Leg day")

            repository.save(renamed)

            assertEquals(renamed, repository.get(legs.id))
            assertEquals(setOf(renamed, arms), repository.saved())
        }

    @Test
    fun `a deleted workout is gone`() =
        runTest {
            val repository = InMemoryWorkoutRepository(listOf(legs, arms))

            repository.delete(legs.id)

            assertNull(repository.get(legs.id))
            assertEquals(setOf(arms), repository.saved())
        }

    @Test
    fun `an unknown ID reads as null and deleting it does nothing`() =
        runTest {
            val repository = InMemoryWorkoutRepository(listOf(legs))

            assertNull(repository.get("unknown"))
            repository.delete("unknown")

            assertEquals(setOf(legs), repository.saved())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `workouts emits after each change`() =
        runTest {
            val repository = InMemoryWorkoutRepository()
            val emissions = mutableListOf<List<Workout>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.workouts.toList(emissions) }
            val renamed = legs.copy(name = "Leg day")

            repository.save(legs)
            repository.save(arms)
            repository.save(renamed)
            repository.delete(legs.id)

            assertEquals(
                listOf(emptySet(), setOf(legs), setOf(legs, arms), setOf(renamed, arms), setOf(arms)),
                emissions.map { it.toSet() },
            )
        }
}
