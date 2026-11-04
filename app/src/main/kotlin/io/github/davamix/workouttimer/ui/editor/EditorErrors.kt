package io.github.davamix.workouttimer.ui.editor

import io.github.davamix.workouttimer.core.model.TimeField
import io.github.davamix.workouttimer.core.model.ValidationError

/**
 * The errors of `validate()` (WM-7), arranged by the field that shows them (P6 SPEC §4.3.4). Rounds and exercises are
 * identified by ID. Plain Kotlin with no Android.
 *
 * @property roundNameMissing IDs of the rounds whose name is missing.
 * @property roundHasNoExercises IDs of the rounds that have no exercises.
 * @property exerciseNameMissing IDs of the exercises whose name is missing.
 * @property timeOutOfRange For each duration or rest out of range, the range it must be in.
 */
data class EditorErrors(
    val workoutNameMissing: Boolean = false,
    val noRounds: Boolean = false,
    val roundNameMissing: Set<String> = emptySet(),
    val roundHasNoExercises: Set<String> = emptySet(),
    val exerciseNameMissing: Set<String> = emptySet(),
    val timeOutOfRange: Map<TimeField, IntRange> = emptyMap(),
) {
    /** Whether there are no errors at all. */
    val isEmpty: Boolean get() = this == NONE

    companion object {
        /** No errors: the workout is valid, or errors aren't shown yet. */
        val NONE = EditorErrors()
    }
}

/** These validation errors, arranged by field. */
fun List<ValidationError>.toEditorErrors(): EditorErrors {
    var workoutNameMissing = false
    var noRounds = false
    val roundNameMissing = mutableSetOf<String>()
    val roundHasNoExercises = mutableSetOf<String>()
    val exerciseNameMissing = mutableSetOf<String>()
    val timeOutOfRange = mutableMapOf<TimeField, IntRange>()
    for (error in this) {
        when (error) {
            ValidationError.WorkoutNameMissing -> workoutNameMissing = true
            ValidationError.NoRounds -> noRounds = true
            is ValidationError.RoundNameMissing -> roundNameMissing += error.roundId
            is ValidationError.RoundHasNoExercises -> roundHasNoExercises += error.roundId
            is ValidationError.ExerciseNameMissing -> exerciseNameMissing += error.exerciseId
            is ValidationError.TimeOutOfRange -> timeOutOfRange[error.field] = error.min..error.max
        }
    }
    return EditorErrors(workoutNameMissing, noRounds, roundNameMissing, roundHasNoExercises, exerciseNameMissing, timeOutOfRange)
}
