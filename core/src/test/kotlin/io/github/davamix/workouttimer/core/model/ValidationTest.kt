package io.github.davamix.workouttimer.core.model

import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidationTest {
    private val valid =
        workout(
            round("A", exercise("A1"), exercise("A2", duration = 30, rest = 5), rest = 30),
            round("B", exercise("B1")),
        )

    private fun durationError(field: TimeField) = ValidationError.TimeOutOfRange(field, Limits.MIN_DURATION_SECONDS, Limits.MAX_SECONDS)

    private fun restError(field: TimeField) = ValidationError.TimeOutOfRange(field, Limits.MIN_REST_SECONDS, Limits.MAX_SECONDS)

    @Test
    fun `the limits match WM-7`() {
        assertEquals(1, Limits.MIN_DURATION_SECONDS)
        assertEquals(0, Limits.MIN_REST_SECONDS)
        assertEquals(99 * 60 + 59, Limits.MAX_SECONDS)
    }

    @Test
    fun `a valid workout gives no errors`() {
        assertEquals(emptyList<ValidationError>(), valid.validate())
    }

    // Names

    @Test
    fun `the workout needs a name`() {
        assertEquals(listOf(ValidationError.WorkoutNameMissing), valid.copy(name = "").validate())
    }

    @Test
    fun `a blank workout name counts as missing`() {
        assertEquals(listOf(ValidationError.WorkoutNameMissing), valid.copy(name = "  ").validate())
    }

    @Test
    fun `each round needs a name`() {
        val workout = workout(round("", exercise("E"), id = "r1"))

        assertEquals(listOf(ValidationError.RoundNameMissing("r1")), workout.validate())
    }

    @Test
    fun `each exercise needs a name`() {
        val workout = workout(round("R", exercise(" ", id = "e1")))

        assertEquals(listOf(ValidationError.ExerciseNameMissing("e1")), workout.validate())
    }

    // Structure

    @Test
    fun `a workout needs at least one round`() {
        assertEquals(listOf(ValidationError.NoRounds), valid.copy(rounds = emptyList()).validate())
    }

    @Test
    fun `a round needs at least one exercise`() {
        val workout = workout(round("R", id = "r1"))

        assertEquals(listOf(ValidationError.RoundHasNoExercises("r1")), workout.validate())
    }

    // Durations: at least 1 s, at most 99:59

    @Test
    fun `the default duration is at least 1 s`() {
        assertEquals(emptyList<ValidationError>(), valid.copy(defaultExerciseDurationSeconds = 1).validate())
        assertEquals(
            listOf(durationError(TimeField.DefaultExerciseDuration)),
            valid.copy(defaultExerciseDurationSeconds = 0).validate(),
        )
    }

    @Test
    fun `an own exercise duration is at least 1 s`() {
        assertEquals(emptyList<ValidationError>(), workout(round("R", exercise("E", duration = 1))).validate())
        assertEquals(
            listOf(durationError(TimeField.ExerciseDuration("e1"))),
            workout(round("R", exercise("E", duration = 0, id = "e1"))).validate(),
        )
        assertEquals(
            listOf(durationError(TimeField.ExerciseDuration("e1"))),
            workout(round("R", exercise("E", duration = -5, id = "e1"))).validate(),
        )
    }

    @Test
    fun `durations are at most 99 59`() {
        assertEquals(
            emptyList<ValidationError>(),
            workout(round("R", exercise("E", duration = 5999)), defaultDuration = 5999).validate(),
        )
        assertEquals(
            listOf(
                durationError(TimeField.DefaultExerciseDuration),
                durationError(TimeField.ExerciseDuration("e1")),
            ),
            workout(round("R", exercise("E", duration = 6000, id = "e1")), defaultDuration = 6000).validate(),
        )
    }

    // Rests: 0 s or more, at most 99:59

    @Test
    fun `default rests are 0 s or more`() {
        assertEquals(
            emptyList<ValidationError>(),
            valid.copy(defaultExerciseRestSeconds = 0, defaultRoundRestSeconds = 0).validate(),
        )
        assertEquals(
            listOf(restError(TimeField.DefaultExerciseRest), restError(TimeField.DefaultRoundRest)),
            valid.copy(defaultExerciseRestSeconds = -1, defaultRoundRestSeconds = -1).validate(),
        )
    }

    @Test
    fun `own rests are 0 s or more`() {
        assertEquals(
            emptyList<ValidationError>(),
            workout(round("R", exercise("E1", rest = 0), exercise("E2"), rest = 0), round("S", exercise("E3"))).validate(),
        )
        assertEquals(
            listOf(restError(TimeField.RoundRest("r1")), restError(TimeField.ExerciseRest("e1"))),
            workout(
                round("R", exercise("E1", rest = -1, id = "e1"), exercise("E2"), rest = -1, id = "r1"),
                round("S", exercise("E3")),
            ).validate(),
        )
    }

    @Test
    fun `rests are at most 99 59`() {
        assertEquals(
            emptyList<ValidationError>(),
            workout(
                round("R", exercise("E1", rest = 5999), exercise("E2"), rest = 5999),
                round("S", exercise("E3")),
                defaultExerciseRest = 5999,
                defaultRoundRest = 5999,
            ).validate(),
        )
        assertEquals(
            listOf(
                restError(TimeField.DefaultExerciseRest),
                restError(TimeField.DefaultRoundRest),
                restError(TimeField.RoundRest("r1")),
                restError(TimeField.ExerciseRest("e1")),
            ),
            workout(
                round("R", exercise("E1", rest = 6000, id = "e1"), exercise("E2"), rest = 6000, id = "r1"),
                round("S", exercise("E3")),
                defaultExerciseRest = 6000,
                defaultRoundRest = 6000,
            ).validate(),
        )
    }

    // Unused values (§4.2) are still validated

    @Test
    fun `the unused rest of the last exercise of a round is validated`() {
        val workout = workout(round("R", exercise("E1"), exercise("Last", rest = 6000, id = "last")))

        assertEquals(listOf(restError(TimeField.ExerciseRest("last"))), workout.validate())
    }

    @Test
    fun `the unused rest of the last round is validated`() {
        val workout = workout(round("R", exercise("E1")), round("Last", exercise("E2"), rest = -1, id = "last"))

        assertEquals(listOf(restError(TimeField.RoundRest("last"))), workout.validate())
    }

    // Several problems

    @Test
    fun `every problem is reported, in workout order`() {
        val workout =
            workout(
                round("", exercise("", duration = 0, rest = -1, id = "e1"), id = "r1", rest = 6000),
                round("Empty", id = "r2"),
                round("R3", exercise("Ok"), exercise("", id = "e3")),
                name = " ",
                defaultDuration = 0,
                defaultExerciseRest = -1,
                defaultRoundRest = 6000,
            )

        assertEquals(
            listOf(
                ValidationError.WorkoutNameMissing,
                durationError(TimeField.DefaultExerciseDuration),
                restError(TimeField.DefaultExerciseRest),
                restError(TimeField.DefaultRoundRest),
                ValidationError.RoundNameMissing("r1"),
                restError(TimeField.RoundRest("r1")),
                ValidationError.ExerciseNameMissing("e1"),
                durationError(TimeField.ExerciseDuration("e1")),
                restError(TimeField.ExerciseRest("e1")),
                ValidationError.RoundHasNoExercises("r2"),
                ValidationError.ExerciseNameMissing("e3"),
            ),
            workout.validate(),
        )
    }

    @Test
    fun `an empty workout reports its name and no rounds`() {
        val workout = workout(name = "")

        assertEquals(listOf(ValidationError.WorkoutNameMissing, ValidationError.NoRounds), workout.validate())
    }
}
