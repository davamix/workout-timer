package io.github.davamix.workouttimer.core.timeline

/** Length of the get-ready countdown before the first exercise (EX-7). */
const val GET_READY_SECONDS = 3

/**
 * One exercise as it appears in the run, with everything the runner screen (UI-1, UI-2, UI-6) and the cues need.
 *
 * Numbers are 1-based and count every round and exercise of the workout, so they read as "round *n* of *m*" and
 * "exercise *n* of *m*".
 *
 * @property exerciseId [io.github.davamix.workouttimer.core.model.Exercise.id] of the exercise.
 * @property name Exercise name.
 * @property durationSeconds Effective duration: the exercise's own, or the workout default.
 * @property roundId [io.github.davamix.workouttimer.core.model.Round.id] of its round.
 * @property roundName Name of its round.
 * @property roundNumber Position of its round in the workout, from 1.
 * @property roundCount Number of rounds in the workout.
 * @property numberInRound Position of the exercise in its round, from 1.
 * @property countInRound Number of exercises in its round.
 */
data class ExerciseInfo(
    val exerciseId: String,
    val name: String,
    val durationSeconds: Int,
    val roundId: String,
    val roundName: String,
    val roundNumber: Int,
    val roundCount: Int,
    val numberInRound: Int,
    val countInRound: Int,
) {
    /** Whether this is the first exercise of its round. */
    val isFirstInRound: Boolean get() = numberInRound == 1

    /** Whether this is the last exercise of its round: the round-end sound plays after it, unless it ends the workout. */
    val isLastInRound: Boolean get() = numberInRound == countInRound

    /** Whether this is the last exercise of the workout: the workout ends after it (§4.3 rule 5). */
    val isLastInWorkout: Boolean get() = isLastInRound && roundNumber == roundCount
}

/** One step of a [Timeline]: the get-ready countdown, an exercise or a rest. */
sealed interface Phase {
    /** How long the phase runs, in seconds. Always at least 1. */
    val durationSeconds: Int

    /** The countdown before the first exercise (EX-7). It lasts [GET_READY_SECONDS] and shows the [first] exercise. */
    data class GetReady(
        val first: ExerciseInfo,
    ) : Phase {
        override val durationSeconds: Int get() = GET_READY_SECONDS
    }

    /** An exercise. It lasts the exercise's effective duration. */
    data class Exercise(
        val exercise: ExerciseInfo,
    ) : Phase {
        override val durationSeconds: Int get() = exercise.durationSeconds
    }

    /**
     * A rest between two exercises (EX-3). Rests of 0 s produce no phase, so [durationSeconds] is always at least 1.
     *
     * @property kind Whether it's the rest after an exercise or after a round.
     * @property previous The exercise before the rest.
     * @property next The exercise after the rest, shown during it (UI-5).
     */
    data class Rest(
        val kind: RestKind,
        override val durationSeconds: Int,
        val previous: ExerciseInfo,
        val next: ExerciseInfo,
    ) : Phase {
        init {
            require(durationSeconds >= 1) { "A rest phase lasts at least 1 s, was $durationSeconds" }
        }
    }
}

/** Which rest a [Phase.Rest] is. */
enum class RestKind {
    /** The rest after an exercise, inside a round (§4.3 rule 3). */
    EXERCISE,

    /** The rest after the last exercise of a round, before the next round (§4.3 rule 4). */
    ROUND,
}
