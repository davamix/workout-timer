package io.github.davamix.workouttimer.core.timeline

import io.github.davamix.workouttimer.core.exampleWorkout
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.timeline.NavigationTarget.Complete
import io.github.davamix.workouttimer.core.timeline.NavigationTarget.GoTo
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Every cell of the specification §5.1 Next/Previous table (EX-9). */
class NavigationTest {
    // §4.4 example, by index:
    // 0 Get ready, 1 Jumping jacks, 2 rest, 3 Arm circles, 4 rest, 5 High knees, 6 round rest, 7 Plank, 8 rest,
    // 9 Crunches.
    private val example = Timeline.of(exampleWorkout)

    // A single exercise: 0 Get ready, 1 Only.
    private val single = Timeline.of(workout(round("R", exercise("Only"))))

    @Test
    fun `the example phases are at the indexes these tests use`() {
        val kinds = example.phases.map { it::class.simpleName }

        assertEquals(
            listOf("GetReady", "Exercise", "Rest", "Exercise", "Rest", "Exercise", "Rest", "Exercise", "Rest", "Exercise"),
            kinds,
        )
    }

    // Get ready

    @Test
    fun `next during get ready goes to the first exercise`() {
        assertEquals(GoTo(1), example.next(0))
        assertEquals(GoTo(1), single.next(0))
    }

    @Test
    fun `previous during get ready is not available`() {
        assertNull(example.previous(0))
        assertNull(single.previous(0))
    }

    // An exercise

    @Test
    fun `next during an exercise goes to the next exercise, skipping the rest`() {
        assertEquals(GoTo(3), example.next(1))
        assertEquals(GoTo(5), example.next(3))
        assertEquals(GoTo(7), example.next(5)) // across the round rest
        assertEquals(GoTo(9), example.next(7))
    }

    @Test
    fun `next during the last exercise ends the workout`() {
        assertEquals(Complete, example.next(9))
        assertEquals(Complete, single.next(1))
    }

    @Test
    fun `previous during an exercise goes to the previous exercise, skipping the rest`() {
        assertEquals(1, example.previous(3))
        assertEquals(3, example.previous(5))
        assertEquals(5, example.previous(7)) // across the round rest
        assertEquals(7, example.previous(9))
    }

    @Test
    fun `previous during the first exercise restarts it`() {
        assertEquals(1, example.previous(1))
        assertEquals(1, single.previous(1))
    }

    // A rest

    @Test
    fun `next during a rest goes to the exercise after it`() {
        assertEquals(GoTo(3), example.next(2))
        assertEquals(GoTo(5), example.next(4))
        assertEquals(GoTo(7), example.next(6)) // round rest
        assertEquals(GoTo(9), example.next(8))
    }

    @Test
    fun `previous during a rest goes to the exercise before it`() {
        assertEquals(1, example.previous(2))
        assertEquals(3, example.previous(4))
        assertEquals(5, example.previous(6)) // round rest
        assertEquals(7, example.previous(8))
    }

    // Without rests, exercises are next to each other

    @Test
    fun `navigation works when there are no rest phases`() {
        val timeline =
            Timeline.of(
                workout(
                    round("One", exercise("A"), exercise("B")),
                    round("Two", exercise("C")),
                    defaultExerciseRest = 0,
                    defaultRoundRest = 0,
                ),
            )

        assertEquals(listOf(GoTo(1), GoTo(2), GoTo(3), Complete), timeline.phases.indices.map { timeline.next(it) })
        assertEquals(listOf(null, 1, 1, 2), timeline.phases.indices.map { timeline.previous(it) })
    }

    // General properties

    @Test
    fun `every target is an exercise phase`() {
        for (timeline in listOf(example, single)) {
            for (index in timeline.phases.indices) {
                (timeline.next(index) as? GoTo)?.let {
                    assertInstanceOf(Phase.Exercise::class.java, timeline.phases[it.phaseIndex])
                }
                timeline.previous(index)?.let { assertInstanceOf(Phase.Exercise::class.java, timeline.phases[it]) }
            }
        }
    }

    @Test
    fun `an index out of range is rejected`() {
        assertThrows<IndexOutOfBoundsException> { example.next(-1) }
        assertThrows<IndexOutOfBoundsException> { example.next(10) }
        assertThrows<IndexOutOfBoundsException> { example.previous(-1) }
        assertThrows<IndexOutOfBoundsException> { example.previous(10) }
    }
}
