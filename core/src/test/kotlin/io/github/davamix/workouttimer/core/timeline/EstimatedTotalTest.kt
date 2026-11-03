package io.github.davamix.workouttimer.core.timeline

import io.github.davamix.workouttimer.core.exampleWorkout
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.model.Limits
import io.github.davamix.workouttimer.core.model.validate
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EstimatedTotalTest {
    // Valid workouts: the same as the timeline

    @Test
    fun `the example estimates 4 23`() {
        assertEquals(263, exampleWorkout.estimatedTotalSeconds())
    }

    @Test
    fun `for valid workouts it equals the timeline total`() {
        val workouts =
            listOf(
                exampleWorkout,
                workout(round("R", exercise("Only"))),
                workout(round("R", exercise("A", rest = 0), exercise("B")), round("S", exercise("C"), rest = 0)),
                workout(
                    round("One", exercise("A", rest = 7), exercise("B", rest = 99), rest = 11),
                    round("Two", exercise("C", duration = 1), rest = 0),
                    round("Three", exercise("D", rest = 5999), exercise("E", duration = 5999)),
                    defaultDuration = 1,
                    defaultExerciseRest = 0,
                    defaultRoundRest = 5999,
                ),
                workout(
                    round("Max", exercise("A", duration = Limits.MAX_SECONDS, rest = Limits.MAX_SECONDS), rest = 5999),
                    round("Max 2", exercise("B", duration = Limits.MAX_SECONDS)),
                ),
            )

        for (workout in workouts) {
            assertTrue(workout.validate().isEmpty(), "Fixture must be valid: ${workout.validate()}")
            assertEquals(Timeline.of(workout).totalSeconds, workout.estimatedTotalSeconds(), "Workout: $workout")
        }
    }

    // Incomplete workouts

    @Test
    fun `a workout with no rounds gives 0`() {
        assertEquals(0, workout().estimatedTotalSeconds())
    }

    @Test
    fun `a workout with only empty rounds gives 0`() {
        assertEquals(0, workout(round("A"), round("B")).estimatedTotalSeconds())
    }

    @Test
    fun `an empty last round is ignored, so the round before it has no round rest`() {
        val workout = workout(round("A", exercise("A1")), round("Empty"), defaultRoundRest = 60)

        assertEquals(3 + 40, workout.estimatedTotalSeconds())
    }

    @Test
    fun `an empty round in the middle is ignored`() {
        val workout = workout(round("A", exercise("A1")), round("Empty", rest = 99), round("B", exercise("B1")))

        assertEquals(3 + 40 + 60 + 40, workout.estimatedTotalSeconds())
    }

    @Test
    fun `an empty first round is ignored`() {
        val workout = workout(round("Empty"), round("A", exercise("A1")))

        assertEquals(3 + 40, workout.estimatedTotalSeconds())
    }

    @Test
    fun `negative durations count as 0`() {
        val workout = workout(round("R", exercise("Own", duration = -10), exercise("Default")), defaultDuration = -40)

        assertEquals(3 + 0 + 15 + 0, workout.estimatedTotalSeconds())
    }

    @Test
    fun `negative rests count as 0`() {
        val workout =
            workout(
                round("One", exercise("A", rest = -5), exercise("B"), exercise("C"), rest = -30),
                round("Two", exercise("D")),
                round("Three", exercise("E")),
                defaultExerciseRest = -15,
                defaultRoundRest = -60,
            )

        assertEquals(3 + 5 * 40, workout.estimatedTotalSeconds())
    }

    @Test
    fun `missing names and out-of-range values don't stop the estimate`() {
        val workout = workout(round("", exercise("", duration = 6000)), name = "")

        assertEquals(3 + 6000, workout.estimatedTotalSeconds())
    }

    @Test
    fun `a total too large for an Int gives Int MAX_VALUE`() {
        val workout = workout(round("R", exercise("A", duration = Int.MAX_VALUE), exercise("B", duration = Int.MAX_VALUE)))

        assertEquals(Int.MAX_VALUE, workout.estimatedTotalSeconds())
    }
}
