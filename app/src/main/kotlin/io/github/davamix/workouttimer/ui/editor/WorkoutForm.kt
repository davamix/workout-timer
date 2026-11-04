package io.github.davamix.workouttimer.ui.editor

import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.newId

// The editor's working copy of a workout (P6 SPEC §4.2). Plain Kotlin with no Android, so it's tested on the JVM.

/**
 * The editor's working copy of a [Workout]. Names hold the text as typed. Time fields hold the digits typed (see
 * [timeText]); `""` means not set. [toWorkout] turns it back into a workout.
 *
 * @property defaultExerciseDuration Required: an empty default is reported by validation.
 */
data class WorkoutForm(
    val id: String,
    val name: String,
    val defaultExerciseDuration: String,
    val defaultExerciseRest: String,
    val defaultRoundRest: String,
    val rounds: List<RoundForm>,
)

/** One round of a [WorkoutForm]. [rest] is the own round rest: `""` when it uses the workout default. */
data class RoundForm(
    val id: String,
    val name: String,
    val rest: String,
    val exercises: List<ExerciseForm>,
)

/** One exercise of a [RoundForm]. [duration] and [rest] are own values: `""` when they use the workout default. */
data class ExerciseForm(
    val id: String,
    val name: String,
    val duration: String,
    val rest: String,
)

/** The three workout defaults, as named by [withDefault]. */
enum class DefaultField { EXERCISE_DURATION, EXERCISE_REST, ROUND_REST }

/** The values a new workout starts with (P6 Q2): the specification §4.4 example's defaults. */
object NewWorkoutDefaults {
    const val EXERCISE_DURATION_SECONDS = 40
    const val EXERCISE_REST_SECONDS = 15
    const val ROUND_REST_SECONDS = 60
}

/** The most digits a time field keeps: 9999 s fits, and validation rejects anything over 5999 (99:59). */
const val TIME_FIELD_MAX_DIGITS = 4

/**
 * What [toWorkout] gives for an empty default, or for text that isn't a number. It's outside every allowed range, so
 * `validate()` reports it as out of range, and `estimatedTotalSeconds()` counts it as 0.
 */
internal const val INVALID_SECONDS = -1

/** What a time field keeps of [input]: the digits 0–9 only, at most [TIME_FIELD_MAX_DIGITS]. */
fun timeText(input: String): String = input.filter { it in '0'..'9' }.take(TIME_FIELD_MAX_DIGITS)

/** This workout as an editor form. Own values that aren't set become `""`. */
fun Workout.toForm(): WorkoutForm =
    WorkoutForm(
        id = id,
        name = name,
        defaultExerciseDuration = defaultExerciseDurationSeconds.toString(),
        defaultExerciseRest = defaultExerciseRestSeconds.toString(),
        defaultRoundRest = defaultRoundRestSeconds.toString(),
        rounds =
            rounds.map { round ->
                RoundForm(
                    id = round.id,
                    name = round.name,
                    rest = round.restSeconds.toText(),
                    exercises =
                        round.exercises.map { ExerciseForm(it.id, it.name, it.durationSeconds.toText(), it.restSeconds.toText()) },
                )
            },
    )

/**
 * The workout this form describes. Names are trimmed; an empty own value is `null`, so it uses the default; an empty
 * default is [INVALID_SECONDS]. IDs and order are kept.
 */
fun WorkoutForm.toWorkout(): Workout =
    Workout(
        id = id,
        name = name.trim(),
        defaultExerciseDurationSeconds = defaultExerciseDuration.toDefault(),
        defaultExerciseRestSeconds = defaultExerciseRest.toDefault(),
        defaultRoundRestSeconds = defaultRoundRest.toDefault(),
        rounds =
            rounds.map { round ->
                Round(
                    id = round.id,
                    name = round.name.trim(),
                    restSeconds = round.rest.toOwn(),
                    exercises =
                        round.exercises.map {
                            Exercise(it.id, it.name.trim(), durationSeconds = it.duration.toOwn(), restSeconds = it.rest.toOwn())
                        },
                )
            },
    )

private fun Int?.toText(): String = this?.toString() ?: ""

private fun String.toDefault(): Int = toIntOrNull() ?: INVALID_SECONDS

private fun String.toOwn(): Int? = if (isEmpty()) null else toIntOrNull() ?: INVALID_SECONDS

/**
 * A new workout's form (P6 Q2): no name, the [NewWorkoutDefaults], and one round called [firstRoundName] with one
 * exercise to name.
 */
fun newWorkoutForm(
    firstRoundName: String,
    newId: () -> String = ::newId,
): WorkoutForm =
    WorkoutForm(
        id = newId(),
        name = "",
        defaultExerciseDuration = NewWorkoutDefaults.EXERCISE_DURATION_SECONDS.toString(),
        defaultExerciseRest = NewWorkoutDefaults.EXERCISE_REST_SECONDS.toString(),
        defaultRoundRest = NewWorkoutDefaults.ROUND_REST_SECONDS.toString(),
        rounds = listOf(newRoundForm(firstRoundName, newId)),
    )

private fun newRoundForm(
    name: String,
    newId: () -> String,
) = RoundForm(id = newId(), name = name, rest = "", exercises = listOf(newExerciseForm(newId)))

private fun newExerciseForm(newId: () -> String) = ExerciseForm(id = newId(), name = "", duration = "", rest = "")

// Edit operations (P6 SPEC §4.2). Each returns a new form. An ID that isn't in the form leaves it as it is.

/** Sets the workout's name, as typed. */
fun WorkoutForm.withName(name: String): WorkoutForm = copy(name = name)

/** Sets one of the workout defaults to [timeText] of [input]. */
fun WorkoutForm.withDefault(
    field: DefaultField,
    input: String,
): WorkoutForm {
    val text = timeText(input)
    return when (field) {
        DefaultField.EXERCISE_DURATION -> copy(defaultExerciseDuration = text)
        DefaultField.EXERCISE_REST -> copy(defaultExerciseRest = text)
        DefaultField.ROUND_REST -> copy(defaultRoundRest = text)
    }
}

/** Sets the name of the round [roundId], as typed. */
fun WorkoutForm.withRoundName(
    roundId: String,
    name: String,
): WorkoutForm = updateRound(roundId) { it.copy(name = name) }

/** Sets the own rest of the round [roundId] to [timeText] of [input]. `""` links it to the default again. */
fun WorkoutForm.withRoundRest(
    roundId: String,
    input: String,
): WorkoutForm = updateRound(roundId) { it.copy(rest = timeText(input)) }

/** Sets the name of the exercise [exerciseId], as typed. */
fun WorkoutForm.withExerciseName(
    exerciseId: String,
    name: String,
): WorkoutForm = updateExercise(exerciseId) { it.copy(name = name) }

/** Sets the own duration of the exercise [exerciseId] to [timeText] of [input]. `""` links it to the default again. */
fun WorkoutForm.withExerciseDuration(
    exerciseId: String,
    input: String,
): WorkoutForm = updateExercise(exerciseId) { it.copy(duration = timeText(input)) }

/** Sets the own rest of the exercise [exerciseId] to [timeText] of [input]. `""` links it to the default again. */
fun WorkoutForm.withExerciseRest(
    exerciseId: String,
    input: String,
): WorkoutForm = updateExercise(exerciseId) { it.copy(rest = timeText(input)) }

/** Adds a round called [name] at the end, with one exercise to name. */
fun WorkoutForm.addRound(
    name: String,
    newId: () -> String = ::newId,
): WorkoutForm = copy(rounds = rounds + newRoundForm(name, newId))

/** Adds an exercise to name at the end of the round [roundId]. */
fun WorkoutForm.addExercise(
    roundId: String,
    newId: () -> String = ::newId,
): WorkoutForm = updateRound(roundId) { it.copy(exercises = it.exercises + newExerciseForm(newId)) }

/** Moves the round [roundId] [by] places: −1 is up, +1 down. A move past either end does nothing. */
fun WorkoutForm.moveRound(
    roundId: String,
    by: Int,
): WorkoutForm = copy(rounds = rounds.moved(rounds.indexOfFirst { it.id == roundId }, by))

/** Moves the exercise [exerciseId] [by] places within its round: −1 is up, +1 down. A move past either end does nothing. */
fun WorkoutForm.moveExercise(
    exerciseId: String,
    by: Int,
): WorkoutForm =
    updateRoundOf(exerciseId) { round ->
        round.copy(exercises = round.exercises.moved(round.exercises.indexOfFirst { it.id == exerciseId }, by))
    }

/** Puts a copy of the round [roundId] right after it, with the same name and values, and new IDs. */
fun WorkoutForm.duplicateRound(
    roundId: String,
    newId: () -> String = ::newId,
): WorkoutForm {
    val index = rounds.indexOfFirst { it.id == roundId }
    if (index < 0) return this
    val original = rounds[index]
    val copy = original.copy(id = newId(), exercises = original.exercises.map { it.copy(id = newId()) })
    return copy(rounds = rounds.inserted(index + 1, copy))
}

/** Puts a copy of the exercise [exerciseId] right after it in its round, with the same name and values, and a new ID. */
fun WorkoutForm.duplicateExercise(
    exerciseId: String,
    newId: () -> String = ::newId,
): WorkoutForm =
    updateRoundOf(exerciseId) { round ->
        val index = round.exercises.indexOfFirst { it.id == exerciseId }
        round.copy(exercises = round.exercises.inserted(index + 1, round.exercises[index].copy(id = newId())))
    }

/** Removes the round [roundId]. Removing the last one is allowed; validation then reports that there are no rounds. */
fun WorkoutForm.removeRound(roundId: String): WorkoutForm = copy(rounds = rounds.filterNot { it.id == roundId })

/**
 * Removes the exercise [exerciseId]. Removing the last one of a round is allowed; validation then reports the empty
 * round.
 */
fun WorkoutForm.removeExercise(exerciseId: String): WorkoutForm =
    updateRoundOf(exerciseId) { round -> round.copy(exercises = round.exercises.filterNot { it.id == exerciseId }) }

private fun WorkoutForm.updateRound(
    roundId: String,
    change: (RoundForm) -> RoundForm,
): WorkoutForm = copy(rounds = rounds.map { if (it.id == roundId) change(it) else it })

/** Applies [change] to the round that holds the exercise [exerciseId], if any. */
private fun WorkoutForm.updateRoundOf(
    exerciseId: String,
    change: (RoundForm) -> RoundForm,
): WorkoutForm = copy(rounds = rounds.map { round -> if (round.exercises.any { it.id == exerciseId }) change(round) else round })

private fun WorkoutForm.updateExercise(
    exerciseId: String,
    change: (ExerciseForm) -> ExerciseForm,
): WorkoutForm =
    updateRoundOf(exerciseId) { round -> round.copy(exercises = round.exercises.map { if (it.id == exerciseId) change(it) else it }) }

/** This list with the item at [index] moved [by] places. Unchanged if [index] is missing or the move goes past an end. */
private fun <T> List<T>.moved(
    index: Int,
    by: Int,
): List<T> {
    val target = index + by
    if (index !in indices || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

private fun <T> List<T>.inserted(
    index: Int,
    item: T,
): List<T> = toMutableList().apply { add(index, item) }
