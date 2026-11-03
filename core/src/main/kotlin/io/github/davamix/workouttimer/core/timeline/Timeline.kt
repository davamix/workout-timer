package io.github.davamix.workouttimer.core.timeline

import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.durationOf
import io.github.davamix.workouttimer.core.model.restAfter
import io.github.davamix.workouttimer.core.model.validate

/**
 * The ordered phases a workout runs through, from Start to completion (EX-1, specification §4.3).
 *
 * Built by [Timeline.of]. It is immutable and holds no time: running it is the engine's job (P4).
 *
 * @property phases The phases in run order. The first is always a [Phase.GetReady], followed by every exercise,
 *   with a [Phase.Rest] between two exercises wherever the rest is longer than 0 s.
 */
class Timeline private constructor(
    val phases: List<Phase>,
) {
    /** Duration from Start to completion, in seconds: the sum of every phase (WM-6). */
    val totalSeconds: Int = phases.sumOf { it.durationSeconds }

    override fun toString(): String = "Timeline(totalSeconds=$totalSeconds, phases=$phases)"

    companion object {
        /**
         * The timeline of [workout], following the §4.3 sequencing rules:
         *
         * 1. A [Phase.GetReady] of [GET_READY_SECONDS] comes first.
         * 2. Then every exercise of every round, in order.
         * 3. Inside a round, each exercise is followed by its own rest, or the default exercise rest.
         * 4. After the last exercise of a round comes the round rest instead; the exercise's own rest is ignored.
         * 5. Nothing follows the last exercise of the last round.
         * 6. Rests of 0 s produce no phase.
         *
         * @throws IllegalArgumentException if the workout isn't valid. Callers check [validate] first.
         */
        fun of(workout: Workout): Timeline {
            val errors = workout.validate()
            require(errors.isEmpty()) { "Workout ${workout.id} is not valid: $errors" }

            val steps = workout.exerciseSteps()
            val phases =
                buildList {
                    add(Phase.GetReady(steps.first().exercise))
                    steps.forEachIndexed { index, step ->
                        add(Phase.Exercise(step.exercise))
                        if (step.restKind != null && step.restSeconds > 0) {
                            add(Phase.Rest(step.restKind, step.restSeconds, step.exercise, steps[index + 1].exercise))
                        }
                    }
                }
            return Timeline(phases)
        }
    }
}

/**
 * One exercise in run order, with the rest that follows it.
 *
 * @property restKind `null` after the last exercise of the workout.
 * @property restSeconds The effective rest that follows, which may be 0 or, for unvalidated workouts, negative.
 *   0 when [restKind] is `null`.
 */
internal data class ExerciseStep(
    val exercise: ExerciseInfo,
    val restKind: RestKind?,
    val restSeconds: Int,
)

/**
 * Every exercise of this workout in run order, with the rest that follows each (§4.3 rules 2–5). It doesn't
 * validate, so [Timeline.of] and [estimatedTotalSeconds] share the rules. Rounds without exercises are left out,
 * so they don't count as rounds; a valid workout has none.
 */
internal fun Workout.exerciseSteps(): List<ExerciseStep> {
    val rounds = rounds.filter { it.exercises.isNotEmpty() }
    return rounds.flatMapIndexed { roundIndex, round ->
        round.exercises.mapIndexed { exerciseIndex, exercise ->
            val info =
                ExerciseInfo(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    durationSeconds = durationOf(exercise),
                    roundId = round.id,
                    roundName = round.name,
                    roundNumber = roundIndex + 1,
                    roundCount = rounds.size,
                    numberInRound = exerciseIndex + 1,
                    countInRound = round.exercises.size,
                )
            when {
                exerciseIndex < round.exercises.lastIndex -> ExerciseStep(info, RestKind.EXERCISE, restAfter(exercise))
                roundIndex < rounds.lastIndex -> ExerciseStep(info, RestKind.ROUND, restAfter(round))
                else -> ExerciseStep(info, restKind = null, restSeconds = 0)
            }
        }
    }
}
