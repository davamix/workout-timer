package io.github.davamix.workouttimer.core.repository

import io.github.davamix.workouttimer.core.model.Workout
import kotlinx.coroutines.flow.Flow

/**
 * Where saved workouts live. P3 implements it with file storage; [InMemoryWorkoutRepository] is the in-memory
 * version for tests and for phases that run before P3.
 *
 * It doesn't validate: the editor checks [io.github.davamix.workouttimer.core.model.validate] before saving (P6).
 */
interface WorkoutRepository {
    /**
     * Every saved workout, in no particular order. It emits the current list on collection and again after each
     * change. Like any state flow, a slow collector may skip intermediate lists, but it always gets the latest.
     */
    val workouts: Flow<List<Workout>>

    /** The saved workout with this [id], or `null` if there is none. */
    suspend fun get(id: String): Workout?

    /** Saves [workout]: inserts it, or replaces the saved workout with the same [Workout.id]. */
    suspend fun save(workout: Workout)

    /** Deletes the workout with this [id]. Does nothing if there is none. */
    suspend fun delete(id: String)
}
