package io.github.davamix.workouttimer.core.repository

import io.github.davamix.workouttimer.core.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A [WorkoutRepository] kept in memory, for tests and for phases that run before file storage exists (P3).
 * Nothing survives the process. It is safe to use from several coroutines at once.
 *
 * @param initial Workouts it starts with. If two share an ID, the last one is kept.
 */
class InMemoryWorkoutRepository(
    initial: List<Workout> = emptyList(),
) : WorkoutRepository {
    private val byId = MutableStateFlow(initial.associateBy { it.id })

    override val workouts: Flow<List<Workout>> = byId.map { it.values.toList() }

    override suspend fun get(id: String): Workout? = byId.value[id]

    override suspend fun save(workout: Workout) {
        byId.update { it + (workout.id to workout) }
    }

    override suspend fun delete(id: String) {
        byId.update { it - id }
    }
}
