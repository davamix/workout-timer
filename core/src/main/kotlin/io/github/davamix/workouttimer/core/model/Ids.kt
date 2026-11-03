package io.github.davamix.workouttimer.core.model

import java.util.UUID

/** A new unique ID for a workout, round or exercise: a random UUID string. */
fun newId(): String = UUID.randomUUID().toString()

/**
 * A copy of this workout called [name], with new IDs for the workout and for every round and exercise in it.
 * Every other value is kept, including own values and unused ones. Used to duplicate a workout (WM-5, P6).
 *
 * @param newId Source of IDs; tests pass a deterministic one.
 */
fun Workout.duplicate(
    name: String,
    newId: () -> String = ::newId,
): Workout =
    copy(
        id = newId(),
        name = name,
        rounds = rounds.map { it.duplicate(newId) },
    )

/**
 * A copy of this round with a new ID for the round and for every exercise in it. Every other value is kept,
 * including the name. Used to duplicate a round in the editor (WM-4, P6).
 */
fun Round.duplicate(newId: () -> String = ::newId): Round =
    copy(
        id = newId(),
        exercises = exercises.map { it.duplicate(newId) },
    )

/**
 * A copy of this exercise with a new ID. Every other value is kept, including the name. Used to duplicate an
 * exercise in the editor (WM-4, P6).
 */
fun Exercise.duplicate(newId: () -> String = ::newId): Exercise = copy(id = newId())
