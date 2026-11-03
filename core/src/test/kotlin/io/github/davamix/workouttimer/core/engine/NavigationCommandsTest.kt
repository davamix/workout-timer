package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Resumed
import io.github.davamix.workouttimer.core.engine.SessionEvent.RoundEnded
import io.github.davamix.workouttimer.core.engine.SessionStatus.COMPLETED
import io.github.davamix.workouttimer.core.engine.SessionStatus.PAUSED
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.timeline.Timeline
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Next and Previous while paused: every cell of the specification §5.1 table (EX-9, TTS-1). */
class NavigationCommandsTest {
    private val phases = exampleTimeline.phases
    private val singleExercise = timelineOf(round("R", exercise("A", duration = 20)))

    /** A machine for [timeline], run on time from [T0] and paused 1.3 s into the phase that starts at [phaseStart]. */
    private fun pausedIn(
        timeline: Timeline,
        phaseStart: Long,
    ): Pair<SessionMachine, Long> {
        val machine = SessionMachine(timeline)
        machine.start(T0)
        val now = phaseStart + 1_300
        machine.runOnTimeUntil(now)
        machine.pause(now)
        return machine to now
    }

    private fun pausedInExample(phaseIndex: Int) = pausedIn(exampleTimeline, exampleStarts[phaseIndex])

    /** Checks the machine waits, paused and silent, at the start of [target] in [timeline]. */
    private fun assertPausedAtStartOf(
        machine: SessionMachine,
        target: Int,
        timeline: Timeline = exampleTimeline,
    ) {
        val phase = timeline.phases[target]
        assertEquals(SessionState(PAUSED, target, phase, phase.durationSeconds, false, true), machine.state())
        assertNull(machine.nextWakeAt())
    }

    // Every GoTo cell of the table, on the §4.4 example

    @ParameterizedTest(name = "{0}: Next from {1} goes to {2}")
    @CsvSource(
        "get ready, 0, 1",
        "an exercise, 1, 3",
        "an exercise, 3, 5",
        "the last exercise of a round, 5, 7",
        "an exercise, 7, 9",
        "an exercise rest, 2, 3",
        "an exercise rest, 4, 5",
        "a round rest, 6, 7",
        "an exercise rest, 8, 9",
    )
    fun `Next moves to the target exercise and stays paused there`(
        during: String,
        from: Int,
        target: Int,
    ) {
        val (machine, now) = pausedInExample(from)

        assertEquals(emptyList<SessionEvent>(), machine.next(now + 500), during)
        assertPausedAtStartOf(machine, target)
    }

    @ParameterizedTest(name = "{0}: Previous from {1} goes to {2}")
    @CsvSource(
        "the first exercise, 1, 1",
        "an exercise, 3, 1",
        "an exercise, 5, 3",
        "the first exercise of a round, 7, 5",
        "the last exercise, 9, 7",
        "an exercise rest, 2, 1",
        "an exercise rest, 4, 3",
        "a round rest, 6, 5",
        "an exercise rest, 8, 7",
    )
    fun `Previous moves to the target exercise and stays paused there`(
        during: String,
        from: Int,
        target: Int,
    ) {
        val (machine, now) = pausedInExample(from)

        assertEquals(emptyList<SessionEvent>(), machine.previous(now + 500), during)
        assertPausedAtStartOf(machine, target)
    }

    @Test
    fun `Next on the last exercise completes the workout`() {
        val (machine, now) = pausedInExample(9)

        assertEquals(listOf(Completed), machine.next(now))
        assertEquals(SessionState(COMPLETED, 9, phases[9], 0, false, false), machine.state())
        assertNull(machine.nextWakeAt())
        assertEquals(emptyList<SessionEvent>(), machine.resume(now + 1_000))
    }

    @Test
    fun `Previous in get ready is not available and does nothing`() {
        val (machine, now) = pausedInExample(0)
        val state = machine.state()

        assertEquals(false, state.canGoPrevious)
        assertEquals(emptyList<SessionEvent>(), machine.previous(now))
        assertEquals(state, machine.state())
    }

    // The same table on a workout with a single exercise

    @Test
    fun `with a single exercise, Next goes from get ready to it, and from it to completion`() {
        val (fromGetReady, now) = pausedIn(singleExercise, T0)
        fromGetReady.next(now)
        assertPausedAtStartOf(fromGetReady, 1, singleExercise)

        val (fromExercise, later) = pausedIn(singleExercise, T0 + 3_000)
        assertEquals(listOf(Completed), fromExercise.next(later))
    }

    @Test
    fun `with a single exercise, Previous restarts it, and isn't available in get ready`() {
        val (fromExercise, now) = pausedIn(singleExercise, T0 + 3_000)
        assertEquals(19, fromExercise.state().remainingSeconds)
        fromExercise.previous(now)
        assertPausedAtStartOf(fromExercise, 1, singleExercise)

        val (fromGetReady, earlier) = pausedIn(singleExercise, T0)
        assertEquals(emptyList<SessionEvent>(), fromGetReady.previous(earlier))
        assertEquals(0, fromGetReady.state().phaseIndex)
    }

    // Resuming after a move

    @ParameterizedTest(name = "from {0} to {1}")
    @CsvSource("1, 3", "5, 7", "6, 7", "0, 1", "3, 1", "1, 1")
    fun `resuming after a move starts the target exercise, which then runs as if reached on time`(
        from: Int,
        target: Int,
    ) {
        val reference = SessionMachine(exampleTimeline).startAndRunOnTime()
        val (machine, now) = pausedInExample(from)
        if (target > from) machine.next(now) else machine.previous(now)
        val resumeAt = now + 7_000

        assertEquals(listOf(Resumed, PhaseStarted(target, phases[target])), machine.resume(resumeAt))
        val shift = resumeAt - exampleStarts[target]
        assertEquals(
            reference.filter { it.atMillis > exampleStarts[target] }.map { it.copy(atMillis = it.atMillis + shift) },
            machine.runOnTime(),
        )
    }

    @Test
    fun `the target starts only once, so a later pause and resume just resume`() {
        val (machine, now) = pausedInExample(1)
        machine.next(now)
        machine.resume(now + 1_000)
        machine.pause(now + 2_500)

        assertEquals(listOf(Resumed), machine.resume(now + 4_000))
    }

    @Test
    fun `Next and Previous can be pressed several times before resuming`() {
        val (machine, now) = pausedInExample(1)
        machine.next(now)
        machine.next(now)
        machine.next(now)
        assertPausedAtStartOf(machine, 7)

        machine.previous(now)
        assertPausedAtStartOf(machine, 5)
    }

    @Test
    fun `moving across the end of a round never sends round-end`() {
        val (machine, now) = pausedInExample(5)
        machine.next(now)

        val events = machine.resume(now + 1_000) + machine.runOnTime().map { it.event }

        assertEquals(emptyList<RoundEnded>(), events.filterIsInstance<RoundEnded>())
    }

    @Test
    fun `an exercise reached with Previous sends round-end again if it ends a round on time`() {
        val (machine, now) = pausedInExample(6)
        machine.previous(now)
        machine.resume(now)

        assertEquals(
            listOf("round-end 1", "start 6"),
            machine
                .runOnTime()
                .map { it.event }
                .eventCodes()
                .filter { it.startsWith("round-end") || it == "start 6" },
        )
    }
}
