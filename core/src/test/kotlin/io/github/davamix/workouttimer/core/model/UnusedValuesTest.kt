package io.github.davamix.workouttimer.core.model

import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnusedValuesTest {
    private val first = exercise("First")
    private val middle = exercise("Middle")
    private val last = exercise("Last", rest = 20)

    @Test
    fun `the rest of every exercise but the last of a round is used`() {
        val round = round("R", first, middle, last)

        assertTrue(round.isRestUsed(first))
        assertTrue(round.isRestUsed(middle))
    }

    @Test
    fun `the rest of the last exercise of a round is unused`() {
        val round = round("R", first, middle, last)

        assertFalse(round.isRestUsed(last))
    }

    @Test
    fun `the rest of the only exercise of a round is unused`() {
        assertFalse(round("R", first).isRestUsed(first))
    }

    @Test
    fun `a kept exercise rest is used again after the exercise is moved`() {
        val before = round("R", first, middle, last)
        val after = before.copy(exercises = listOf(last, first, middle))

        assertFalse(before.isRestUsed(last))
        assertTrue(after.isRestUsed(last))
        assertEquals(20, workout(after).restAfter(after.exercises.first()))
        assertFalse(after.isRestUsed(middle))
    }

    @Test
    fun `the round rest of every round but the last is used`() {
        val workout = workout(round("A", first), round("B", middle), round("C", last, rest = 90))

        assertTrue(workout.isRestUsed(workout.rounds[0]))
        assertTrue(workout.isRestUsed(workout.rounds[1]))
    }

    @Test
    fun `the round rest of the last round is unused`() {
        val workout = workout(round("A", first), round("B", middle), round("C", last, rest = 90))

        assertFalse(workout.isRestUsed(workout.rounds[2]))
    }

    @Test
    fun `a kept round rest is used again after the round is moved`() {
        val roundA = round("A", first)
        val roundC = round("C", last, rest = 90)
        val before = workout(roundA, roundC)
        val after = before.copy(rounds = listOf(roundC, roundA))

        assertFalse(before.isRestUsed(roundC))
        assertTrue(after.isRestUsed(roundC))
        assertEquals(90, after.restAfter(roundC))
        assertFalse(after.isRestUsed(roundA))
    }

    @Test
    fun `an exercise from another round is rejected`() {
        assertThrows<IllegalArgumentException> { round("R", first).isRestUsed(middle) }
    }

    @Test
    fun `a round from another workout is rejected`() {
        assertThrows<IllegalArgumentException> { workout(round("A", first)).isRestUsed(round("B", middle)) }
    }
}
