package io.github.davamix.workouttimer.core.model

import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EffectiveValuesTest {
    private val linked = exercise("Linked")
    private val own = exercise("Own", duration = 25, rest = 5)
    private val linkedRound = round("Linked round", linked, own)
    private val ownRound = round("Own round", exercise("Other"), rest = 90)
    private val base =
        workout(
            linkedRound,
            ownRound,
            defaultDuration = 40,
            defaultExerciseRest = 15,
            defaultRoundRest = 60,
        )

    @Test
    fun `an unset exercise duration uses the default`() {
        assertEquals(40, base.durationOf(linked))
    }

    @Test
    fun `an own exercise duration overrides the default`() {
        assertEquals(25, base.durationOf(own))
    }

    @Test
    fun `an unset exercise rest uses the default`() {
        assertEquals(15, base.restAfter(linked))
    }

    @Test
    fun `an own exercise rest overrides the default`() {
        assertEquals(5, base.restAfter(own))
    }

    @Test
    fun `an own exercise rest of 0 overrides the default`() {
        assertEquals(0, base.restAfter(exercise("No rest", rest = 0)))
    }

    @Test
    fun `an unset round rest uses the default`() {
        assertEquals(60, base.restAfter(linkedRound))
    }

    @Test
    fun `an own round rest overrides the default`() {
        assertEquals(90, base.restAfter(ownRound))
    }

    @Test
    fun `changing the default duration changes only linked exercises`() {
        val changed = base.copy(defaultExerciseDurationSeconds = 50)

        assertEquals(50, changed.durationOf(linked))
        assertEquals(25, changed.durationOf(own))
    }

    @Test
    fun `changing the default exercise rest changes only linked exercises`() {
        val changed = base.copy(defaultExerciseRestSeconds = 20)

        assertEquals(20, changed.restAfter(linked))
        assertEquals(5, changed.restAfter(own))
    }

    @Test
    fun `changing the default round rest changes only linked rounds`() {
        val changed = base.copy(defaultRoundRestSeconds = 45)

        assertEquals(45, changed.restAfter(linkedRound))
        assertEquals(90, changed.restAfter(ownRound))
    }
}
