package io.github.davamix.workouttimer.core.model

/** Allowed ranges for durations and rests, in seconds (WM-7). */
object Limits {
    /** Shortest exercise duration, for the default and for own values. */
    const val MIN_DURATION_SECONDS = 1

    /** Shortest rest, for defaults and own values. 0 means no rest. */
    const val MIN_REST_SECONDS = 0

    /** Longest duration or rest: 99:59, the most mm:ss can show. */
    const val MAX_SECONDS = 5999
}

/**
 * Checks this workout against the WM-7 rules and returns every problem found, in workout order: the workout's own
 * fields first, then each round followed by its exercises. An empty list means the workout is valid.
 *
 * - The workout, each round and each exercise need a name. A blank name counts as missing.
 * - A workout has at least one round, and a round has at least one exercise.
 * - Durations are from [Limits.MIN_DURATION_SECONDS] to [Limits.MAX_SECONDS]; rests from [Limits.MIN_REST_SECONDS]
 *   to [Limits.MAX_SECONDS].
 *
 * Own values are checked even where they're unused (the last exercise of a round, the last round), because they
 * apply again if the exercise or round is moved (§4.2). Values that aren't set use a default, which is checked once.
 */
fun Workout.validate(): List<ValidationError> =
    buildList {
        if (name.isBlank()) add(ValidationError.WorkoutNameMissing)
        checkDuration(TimeField.DefaultExerciseDuration, defaultExerciseDurationSeconds)
        checkRest(TimeField.DefaultExerciseRest, defaultExerciseRestSeconds)
        checkRest(TimeField.DefaultRoundRest, defaultRoundRestSeconds)
        if (rounds.isEmpty()) add(ValidationError.NoRounds)

        for (round in rounds) {
            if (round.name.isBlank()) add(ValidationError.RoundNameMissing(round.id))
            round.restSeconds?.let { checkRest(TimeField.RoundRest(round.id), it) }
            if (round.exercises.isEmpty()) add(ValidationError.RoundHasNoExercises(round.id))

            for (exercise in round.exercises) {
                if (exercise.name.isBlank()) add(ValidationError.ExerciseNameMissing(exercise.id))
                exercise.durationSeconds?.let { checkDuration(TimeField.ExerciseDuration(exercise.id), it) }
                exercise.restSeconds?.let { checkRest(TimeField.ExerciseRest(exercise.id), it) }
            }
        }
    }

private fun MutableList<ValidationError>.checkDuration(
    field: TimeField,
    seconds: Int,
) = checkRange(field, seconds, Limits.MIN_DURATION_SECONDS)

private fun MutableList<ValidationError>.checkRest(
    field: TimeField,
    seconds: Int,
) = checkRange(field, seconds, Limits.MIN_REST_SECONDS)

private fun MutableList<ValidationError>.checkRange(
    field: TimeField,
    seconds: Int,
    min: Int,
) {
    if (seconds !in min..Limits.MAX_SECONDS) add(ValidationError.TimeOutOfRange(field, min, Limits.MAX_SECONDS))
}

/** One problem found by [validate]. The editor turns each into a message next to the field (P6). */
sealed interface ValidationError {
    /** The workout name is empty or blank. */
    data object WorkoutNameMissing : ValidationError

    /** The workout has no rounds. */
    data object NoRounds : ValidationError

    /** The round [roundId] has an empty or blank name. */
    data class RoundNameMissing(
        val roundId: String,
    ) : ValidationError

    /** The round [roundId] has no exercises. */
    data class RoundHasNoExercises(
        val roundId: String,
    ) : ValidationError

    /** The exercise [exerciseId] has an empty or blank name. */
    data class ExerciseNameMissing(
        val exerciseId: String,
    ) : ValidationError

    /** The duration or rest in [field] is outside [min]..[max] seconds. */
    data class TimeOutOfRange(
        val field: TimeField,
        val min: Int,
        val max: Int,
    ) : ValidationError
}

/** A duration or rest field of the workout, as named by [ValidationError.TimeOutOfRange]. */
sealed interface TimeField {
    /** [Workout.defaultExerciseDurationSeconds]. */
    data object DefaultExerciseDuration : TimeField

    /** [Workout.defaultExerciseRestSeconds]. */
    data object DefaultExerciseRest : TimeField

    /** [Workout.defaultRoundRestSeconds]. */
    data object DefaultRoundRest : TimeField

    /** The own [Exercise.durationSeconds] of the exercise [exerciseId]. */
    data class ExerciseDuration(
        val exerciseId: String,
    ) : TimeField

    /** The own [Exercise.restSeconds] of the exercise [exerciseId]. */
    data class ExerciseRest(
        val exerciseId: String,
    ) : TimeField

    /** The own [Round.restSeconds] of the round [roundId]. */
    data class RoundRest(
        val roundId: String,
    ) : TimeField
}
