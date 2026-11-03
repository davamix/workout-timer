package io.github.davamix.workouttimer.core.model

// Effective values (specification §4.2, "Defaults"): an optional value that isn't set falls back to the workout
// default. Changing a default changes every value linked to it; own values stay as they are.

/** The duration [exercise] runs for: its own duration, or the workout's default exercise duration (WM-1). */
fun Workout.durationOf(exercise: Exercise): Int = exercise.durationSeconds ?: defaultExerciseDurationSeconds

/**
 * The rest after [exercise]: its own rest, or the workout's default exercise rest (WM-2).
 *
 * This is the configured value. It doesn't run after the last exercise of a round; see [Round.isRestUsed].
 */
fun Workout.restAfter(exercise: Exercise): Int = exercise.restSeconds ?: defaultExerciseRestSeconds

/**
 * The rest after [round]: its own round rest, or the workout's default round rest (WM-2).
 *
 * This is the configured value. It doesn't run after the last round; see [Workout.isRestUsed].
 */
fun Workout.restAfter(round: Round): Int = round.restSeconds ?: defaultRoundRestSeconds

// Unused values (specification §4.2): the last exercise of a round has no exercise rest, because the round rest
// runs instead, and the last round has no round rest, because the workout ends. Their values are kept, so they
// apply again if the exercise or round is moved.

/**
 * Whether the exercise rest of [exercise] is used: `false` for the last exercise of this round.
 *
 * The editor marks unused rests as not applicable (P6). Exercises are matched by [Exercise.id].
 *
 * @throws IllegalArgumentException if [exercise] isn't in this round.
 */
fun Round.isRestUsed(exercise: Exercise): Boolean {
    val index = exercises.indexOfFirst { it.id == exercise.id }
    require(index >= 0) { "Exercise ${exercise.id} is not in round $id" }
    return index < exercises.lastIndex
}

/**
 * Whether the round rest of [round] is used: `false` for the last round of this workout.
 *
 * The editor marks unused rests as not applicable (P6). Rounds are matched by [Round.id].
 *
 * @throws IllegalArgumentException if [round] isn't in this workout.
 */
fun Workout.isRestUsed(round: Round): Boolean {
    val index = rounds.indexOfFirst { it.id == round.id }
    require(index >= 0) { "Round ${round.id} is not in workout $id" }
    return index < rounds.lastIndex
}
