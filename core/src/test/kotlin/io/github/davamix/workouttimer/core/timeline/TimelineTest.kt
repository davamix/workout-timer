package io.github.davamix.workouttimer.core.timeline

import io.github.davamix.workouttimer.core.exampleWorkout
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** One test per §4.3 sequencing rule, plus the information phases carry. */
class TimelineTest {
    /** A compact description of a phase: `G3`, `E:name:40`, `R:15` (exercise rest) or `RR:60` (round rest). */
    private fun Phase.code(): String =
        when (this) {
            is Phase.GetReady -> "G$durationSeconds"
            is Phase.Exercise -> "E:${exercise.name}:$durationSeconds"
            is Phase.Rest -> if (kind == RestKind.EXERCISE) "R:$durationSeconds" else "RR:$durationSeconds"
        }

    private fun Timeline.codes() = phases.map { it.code() }

    // Rule 1: get ready

    @Test
    fun `get ready lasts 3 s`() {
        assertEquals(3, GET_READY_SECONDS)
    }

    @Test
    fun `get ready comes first, only once, and knows the first exercise`() {
        val timeline = Timeline.of(exampleWorkout)

        val getReady = assertInstanceOf(Phase.GetReady::class.java, timeline.phases.first())
        assertEquals(3, getReady.durationSeconds)
        assertEquals("Jumping jacks", getReady.first.name)
        assertEquals(1, timeline.phases.count { it is Phase.GetReady })
    }

    // Rules 2 and 3: exercises in order, with the exercise rest between them

    @Test
    fun `the exercise rest sits between exercises of a round`() {
        val workout = workout(round("R", exercise("A"), exercise("B", rest = 5), exercise("C")), defaultExerciseRest = 15)

        assertEquals(listOf("G3", "E:A:40", "R:15", "E:B:40", "R:5", "E:C:40"), Timeline.of(workout).codes())
    }

    @Test
    fun `exercises take their effective duration`() {
        val workout = workout(round("R", exercise("Own", duration = 25), exercise("Default")), defaultDuration = 45)

        assertEquals(listOf("G3", "E:Own:25", "R:15", "E:Default:45"), Timeline.of(workout).codes())
    }

    // Rule 4: the round rest replaces the last exercise's rest

    @Test
    fun `the round rest replaces the last exercise's own rest`() {
        val workout =
            workout(
                round("One", exercise("A"), exercise("B", rest = 5), rest = 30),
                round("Two", exercise("C")),
            )

        assertEquals(listOf("G3", "E:A:40", "R:15", "E:B:40", "RR:30", "E:C:40"), Timeline.of(workout).codes())
    }

    @Test
    fun `the round rest uses the default when the round has none`() {
        val workout = workout(round("One", exercise("A")), round("Two", exercise("B")), defaultRoundRest = 60)

        assertEquals(listOf("G3", "E:A:40", "RR:60", "E:B:40"), Timeline.of(workout).codes())
    }

    // Rule 5: nothing after the last exercise of the workout

    @Test
    fun `no rest follows the last exercise of the workout`() {
        val workout =
            workout(
                round("One", exercise("A")),
                round("Two", exercise("B"), exercise("C", rest = 20), rest = 90),
            )

        val timeline = Timeline.of(workout)

        assertEquals(listOf("G3", "E:A:40", "RR:60", "E:B:40", "R:15", "E:C:40"), timeline.codes())
        assertInstanceOf(Phase.Exercise::class.java, timeline.phases.last())
    }

    // Rule 6: 0 s rests are skipped

    @Test
    fun `a 0 s exercise rest produces no phase`() {
        val workout = workout(round("R", exercise("A", rest = 0), exercise("B"), exercise("C")), defaultExerciseRest = 0)

        assertEquals(listOf("G3", "E:A:40", "E:B:40", "E:C:40"), Timeline.of(workout).codes())
    }

    @Test
    fun `a 0 s round rest produces no phase`() {
        val workout =
            workout(
                round("One", exercise("A"), rest = 0),
                round("Two", exercise("B")),
                round("Three", exercise("C")),
                defaultRoundRest = 0,
            )

        assertEquals(listOf("G3", "E:A:40", "E:B:40", "E:C:40"), Timeline.of(workout).codes())
    }

    @Test
    fun `an own rest overrides a 0 s default, and an own 0 s rest overrides a default`() {
        val workout =
            workout(
                round("One", exercise("A", rest = 10), exercise("B", rest = 0), exercise("C")),
                round("Two", exercise("D")),
                defaultExerciseRest = 0,
                defaultRoundRest = 0,
            )

        assertEquals(listOf("G3", "E:A:40", "R:10", "E:B:40", "E:C:40", "E:D:40"), Timeline.of(workout).codes())
    }

    // Edge cases

    @Test
    fun `a single-exercise workout gives get ready and the exercise`() {
        val workout = workout(round("R", exercise("Only", rest = 30), rest = 60))

        val timeline = Timeline.of(workout)

        assertEquals(listOf("G3", "E:Only:40"), timeline.codes())
        assertEquals(43, timeline.totalSeconds)
    }

    @Test
    fun `the total is the sum of every phase`() {
        val timeline = Timeline.of(exampleWorkout)

        assertEquals(timeline.phases.sumOf { it.durationSeconds }, timeline.totalSeconds)
    }

    @Test
    fun `an invalid workout is rejected`() {
        assertThrows<IllegalArgumentException> { Timeline.of(workout()) }
        assertThrows<IllegalArgumentException> { Timeline.of(workout(round("R"))) }
        assertThrows<IllegalArgumentException> { Timeline.of(workout(round("R", exercise("A", duration = 0)))) }
    }

    @Test
    fun `a rest phase cannot be shorter than 1 s`() {
        val info = (Timeline.of(exampleWorkout).phases[1] as Phase.Exercise).exercise

        assertThrows<IllegalArgumentException> { Phase.Rest(RestKind.EXERCISE, 0, info, info) }
    }

    // What phases know

    @Test
    fun `exercise info numbers rounds and exercises from 1`() {
        val infos =
            Timeline
                .of(exampleWorkout)
                .phases
                .filterIsInstance<Phase.Exercise>()
                .map { it.exercise }

        assertEquals(
            listOf(
                ExerciseInfo("e-Jumping jacks", "Jumping jacks", 30, "r-Warm-up", "Warm-up", 1, 2, 1, 3),
                ExerciseInfo("e-Arm circles", "Arm circles", 10, "r-Warm-up", "Warm-up", 1, 2, 2, 3),
                ExerciseInfo("e-High knees", "High knees", 40, "r-Warm-up", "Warm-up", 1, 2, 3, 3),
                ExerciseInfo("e-Plank", "Plank", 60, "r-Core", "Core", 2, 2, 1, 2),
                ExerciseInfo("e-Crunches", "Crunches", 40, "r-Core", "Core", 2, 2, 2, 2),
            ),
            infos,
        )
    }

    @Test
    fun `exercise info flags mark the first and last of a round and the last of the workout`() {
        val infos =
            Timeline
                .of(exampleWorkout)
                .phases
                .filterIsInstance<Phase.Exercise>()
                .map { it.exercise }

        assertEquals(listOf(true, false, false, true, false), infos.map { it.isFirstInRound })
        assertEquals(listOf(false, false, true, false, true), infos.map { it.isLastInRound })
        assertEquals(listOf(false, false, false, false, true), infos.map { it.isLastInWorkout })
    }

    @Test
    fun `the only exercise is first and last in its round and in the workout`() {
        val info =
            Timeline
                .of(workout(round("R", exercise("Only"))))
                .phases
                .filterIsInstance<Phase.Exercise>()
                .single()
                .exercise

        assertTrue(info.isFirstInRound)
        assertTrue(info.isLastInRound)
        assertTrue(info.isLastInWorkout)
    }

    @Test
    fun `the last exercise of a round that isn't the last does not end the workout`() {
        val workout = workout(round("One", exercise("A")), round("Two", exercise("B")))
        val first =
            Timeline
                .of(workout)
                .phases
                .filterIsInstance<Phase.Exercise>()
                .first()
                .exercise

        assertTrue(first.isLastInRound)
        assertFalse(first.isLastInWorkout)
    }

    @Test
    fun `rest phases know the exercise before and after them`() {
        val phases = Timeline.of(exampleWorkout).phases

        val rests = phases.withIndex().filter { it.value is Phase.Rest }
        assertEquals(listOf(2, 4, 6, 8), rests.map { it.index })
        for ((index, phase) in rests) {
            val rest = phase as Phase.Rest
            assertEquals((phases[index - 1] as Phase.Exercise).exercise, rest.previous)
            assertEquals((phases[index + 1] as Phase.Exercise).exercise, rest.next)
        }
        val roundRest = phases[6] as Phase.Rest
        assertEquals("High knees", roundRest.previous.name)
        assertEquals("Plank", roundRest.next.name)
    }
}
