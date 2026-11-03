package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.Paused
import io.github.davamix.workouttimer.core.engine.SessionEvent.Resumed
import io.github.davamix.workouttimer.core.engine.SessionStatus.PAUSED
import io.github.davamix.workouttimer.core.engine.SessionStatus.RUNNING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** Pause and resume in every kind of phase, to the millisecond (EX-4, EX-5, NF-1, TTS-3). */
class PauseResumeTest {
    private val phases = exampleTimeline.phases

    @Test
    fun `pausing keeps the phase and the milliseconds left, and resuming continues from there`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(exampleStarts[1] + 12_000)
        val pauseAt = exampleStarts[1] + 12_400 // Jumping jacks: 17.6 s left, shown as 18

        assertEquals(listOf(Paused), machine.pause(pauseAt))
        assertEquals(SessionState(PAUSED, 1, phases[1], 18, false, true), machine.state())

        val resumeAt = pauseAt + 60_000
        assertEquals(listOf(Resumed), machine.resume(resumeAt))
        assertEquals(SessionState(RUNNING, 1, phases[1], 18, false, false), machine.state())
        assertEquals(resumeAt + 600, machine.nextWakeAt())
        assertEquals(listOf("1:17"), machine.advanceTo(resumeAt + 600).eventCodes())
        assertEquals(listOf("1:1"), machine.advanceTo(resumeAt + 17_599).eventCodes())
        assertEquals(listOf("start 2"), machine.advanceTo(resumeAt + 17_600).eventCodes())
    }

    @ParameterizedTest(name = "phase {0}")
    @ValueSource(ints = [0, 1, 2, 6]) // the get-ready countdown, an exercise, an exercise rest, a round rest
    fun `a pause in any kind of phase shifts the rest of the run by exactly its length`(phaseIndex: Int) {
        val reference = SessionMachine(exampleTimeline).startAndRunOnTime()
        val pauseAt = exampleStarts[phaseIndex] + 1_300
        val pauseLength = 45_678L
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(pauseAt)

        assertEquals(listOf(Paused), machine.pause(pauseAt))
        assertEquals(phaseIndex, machine.state().phaseIndex)
        assertEquals(listOf(Resumed), machine.resume(pauseAt + pauseLength))
        assertEquals(
            reference.filter { it.atMillis > pauseAt }.map { it.copy(atMillis = it.atMillis + pauseLength) },
            machine.runOnTime(),
        )
    }

    @Test
    fun `nothing happens while paused, however long it lasts`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(T0 + 10_000)
        machine.pause(T0 + 10_000)
        val state = machine.state()

        assertNull(machine.nextWakeAt())
        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(T0 + 3_600_000))
        assertEquals(state, machine.state())
    }

    @Test
    fun `a pause just after a boundary the session hasn't processed lands in the new phase`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.advanceTo(T0 + 2_000)

        assertEquals(listOf("start 1", "paused"), machine.pause(T0 + 3_200).eventCodes())
        assertEquals(SessionState(PAUSED, 1, phases[1], 30, false, true), machine.state())

        machine.resume(T0 + 13_200)
        assertEquals(T0 + 14_000, machine.nextWakeAt()) // 29.8 s were left, so 29 shows 0.8 s after resuming
    }

    @Test
    fun `a pause exactly at a phase end pauses at the start of the next phase`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.advanceTo(T0 + 2_000)

        assertEquals(listOf("start 1", "paused"), machine.pause(T0 + 3_000).eventCodes())
        assertEquals(30, machine.state().remainingSeconds)

        machine.resume(T0 + 5_000)
        assertEquals(listOf("start 2"), machine.advanceTo(T0 + 35_000).eventCodes().takeLast(1))
        assertEquals(2, machine.state().phaseIndex)
    }

    @Test
    fun `resuming replays nothing, and the countdown carries on from the current second`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(exampleStarts[1] + 27_000)
        assertEquals(3, machine.state().remainingSeconds)

        machine.pause(exampleStarts[1] + 27_500)
        val resumeAt = exampleStarts[1] + 40_000

        assertEquals(listOf(Resumed), machine.resume(resumeAt))
        assertEquals(listOf("${resumeAt - T0 + 500} 1:2"), machine.runOnTimeUntil(resumeAt + 500).codes())
    }

    @Test
    fun `many short pauses don't make the run drift`() {
        val reference = SessionMachine(exampleTimeline).startAndRunOnTime().map { it.event }
        val machine = SessionMachine(exampleTimeline)
        val run = machine.start(T0).map { Timed(T0, it) }.toMutableList()
        var now = T0
        var paused = 0L

        while (machine.state().status == RUNNING) {
            now += 1_700
            run += machine.runOnTimeUntil(now)
            if (machine.state().status != RUNNING) break
            run += machine.pause(now).map { Timed(now, it) }
            now += 250
            paused += 250
            run += machine.resume(now).map { Timed(now, it) }
        }

        assertEquals(Timed(T0 + 263_000 + paused, Completed), run.last())
        assertEquals(reference, run.map { it.event }.filter { it != Paused && it != Resumed })
    }
}
