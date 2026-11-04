package io.github.davamix.workouttimer.ui.editor

import io.github.davamix.workouttimer.core.model.TimeField
import io.github.davamix.workouttimer.core.model.ValidationError
import io.github.davamix.workouttimer.core.model.validate
import io.github.davamix.workouttimer.data.exampleWorkout
import io.github.davamix.workouttimer.data.exercise
import io.github.davamix.workouttimer.data.round
import io.github.davamix.workouttimer.data.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditorErrorsTest {
    @Test
    fun `the example has no errors`() {
        val errors = exampleWorkout.validate().toEditorErrors()

        assertTrue(errors.isEmpty)
        assertEquals(EditorErrors.NONE, errors)
    }

    @Test
    fun `each kind of error lands on its field`() {
        val errors =
            listOf(
                ValidationError.WorkoutNameMissing,
                ValidationError.NoRounds,
                ValidationError.RoundNameMissing("r1"),
                ValidationError.RoundHasNoExercises("r2"),
                ValidationError.ExerciseNameMissing("e1"),
                ValidationError.TimeOutOfRange(TimeField.DefaultExerciseDuration, 1, 5999),
                ValidationError.TimeOutOfRange(TimeField.RoundRest("r1"), 0, 5999),
            ).toEditorErrors()

        assertEquals(
            EditorErrors(
                workoutNameMissing = true,
                noRounds = true,
                roundNameMissing = setOf("r1"),
                roundHasNoExercises = setOf("r2"),
                exerciseNameMissing = setOf("e1"),
                timeOutOfRange = mapOf(TimeField.DefaultExerciseDuration to 1..5999, TimeField.RoundRest("r1") to 0..5999),
            ),
            errors,
        )
        assertFalse(errors.isEmpty)
    }

    @Test
    fun `the errors of an invalid workout are all arranged by field`() {
        val invalid =
            workout(
                round(" ", exercise("", duration = 0), exercise("Lunges", rest = 6000), id = "r1"),
                round("Empty", id = "r2"),
                name = "",
                id = "w",
                defaultExerciseRest = -1,
            )

        val errors = invalid.validate().toEditorErrors()

        assertEquals(
            EditorErrors(
                workoutNameMissing = true,
                roundNameMissing = setOf("r1"),
                roundHasNoExercises = setOf("r2"),
                exerciseNameMissing = setOf("e-"),
                timeOutOfRange =
                    mapOf(
                        TimeField.DefaultExerciseRest to 0..5999,
                        TimeField.ExerciseDuration("e-") to 1..5999,
                        TimeField.ExerciseRest("e-lunges") to 0..5999,
                    ),
            ),
            errors,
        )
    }
}
