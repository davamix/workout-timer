package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.Stopped
import io.github.davamix.workouttimer.core.engine.SessionStatus.COMPLETED
import io.github.davamix.workouttimer.core.engine.SessionStatus.PAUSED
import io.github.davamix.workouttimer.core.engine.SessionStatus.RUNNING
import io.github.davamix.workouttimer.core.engine.SessionStatus.STOPPED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Which command applies in which status, and what the others do: nothing (EX-8). */
class SessionCommandsTest {
    private val phases = exampleTimeline.phases

    /** A command of the machine, with its name for messages. */
    private class Command(
        val name: String,
        val call: SessionMachine.(Long) -> List<SessionEvent>,
    )

    private val start = Command("start") { start(it) }
    private val pause = Command("pause") { pause(it) }
    private val resume = Command("resume") { resume(it) }
    private val stop = Command("stop") { stop(it) }

    // A machine in each status, with the clock time of its last call. Calling a command at that time adds no elapsed
    // time, so whatever changes comes from the command itself.

    private fun notStarted() = SessionMachine(exampleTimeline) to T0

    private fun running() =
        SessionMachine(exampleTimeline).apply {
            start(T0)
            runOnTimeUntil(T0 + 5_000)
        } to T0 + 5_000

    private fun paused() = running().also { (machine, now) -> machine.pause(now) }

    private fun completed() = SessionMachine(exampleTimeline).apply { startAndRunOnTime() } to T0 + 263_000

    private fun stopped() = running().also { (machine, now) -> machine.stop(now) }

    private fun assertDoNothing(
        machineAndNow: Pair<SessionMachine, Long>,
        vararg commands: Command,
    ) {
        val (machine, now) = machineAndNow
        for (command in commands) {
            val state = machine.state()
            val wake = machine.nextWakeAt()

            assertEquals(emptyList<SessionEvent>(), machine.(command.call)(now), command.name)
            assertEquals(state, machine.state(), command.name)
            assertEquals(wake, machine.nextWakeAt(), command.name)
        }
    }

    // Commands that don't apply

    @Test
    fun `before start, pause and resume do nothing`() {
        assertDoNothing(notStarted(), pause, resume)
    }

    @Test
    fun `while running, start and resume do nothing`() {
        assertDoNothing(running(), start, resume)
    }

    @Test
    fun `while paused, start and pause do nothing`() {
        assertDoNothing(paused(), start, pause)
    }

    @Test
    fun `once completed, no command does anything`() {
        assertDoNothing(completed(), start, pause, resume, stop)
    }

    @Test
    fun `once stopped, no command does anything, and time has no effect`() {
        val stopped = stopped()
        assertDoNothing(stopped, start, pause, resume, stop)

        val (machine, now) = stopped
        val state = machine.state()
        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(now + 3_600_000))
        assertEquals(state, machine.state())
    }

    // Stop

    @Test
    fun `stop before start ends the session`() {
        val (machine, now) = notStarted()

        assertEquals(listOf(Stopped), machine.stop(now))
        assertEquals(SessionState(STOPPED, 0, phases[0], 3, false, false), machine.state())
        assertNull(machine.nextWakeAt())
    }

    @Test
    fun `stop while running ends the session, keeping the phase and the time it had left`() {
        val (machine, _) = running()

        assertEquals(listOf(Stopped), machine.stop(T0 + 5_400))
        assertEquals(SessionState(STOPPED, 1, phases[1], 28, false, false), machine.state())
        assertNull(machine.nextWakeAt())
    }

    @Test
    fun `stop while paused ends the session`() {
        val (machine, now) = paused()

        assertEquals(listOf(Stopped), machine.stop(now + 10_000))
        assertEquals(SessionState(STOPPED, 1, phases[1], 28, false, false), machine.state())
    }

    // Commands apply elapsed time first

    @Test
    fun `a command that comes after an unprocessed end of the workout finds it completed`() {
        for (command in listOf(pause, stop)) {
            val machine = SessionMachine(exampleTimeline)
            machine.start(T0)
            machine.advanceTo(T0 + 262_000)

            assertEquals(listOf(Completed), machine.(command.call)(T0 + 263_500), command.name)
            assertEquals(COMPLETED, machine.state().status, command.name)
        }
    }

    // The state in each status

    @Test
    fun `the state says whether previous is available`() {
        assertEquals(false, running().first.state().canGoPrevious)
        assertEquals(true, paused().first.state().canGoPrevious)

        val pausedInGetReady = SessionMachine(exampleTimeline)
        pausedInGetReady.start(T0)
        pausedInGetReady.pause(T0 + 500)
        assertEquals(SessionState(PAUSED, 0, phases[0], 3, false, false), pausedInGetReady.state())
    }

    @Test
    fun `the state in each status`() {
        assertEquals(SessionState(RUNNING, 1, phases[1], 28, false, false), running().first.state())
        assertEquals(SessionState(PAUSED, 1, phases[1], 28, false, true), paused().first.state())
        assertEquals(SessionState(COMPLETED, 9, phases[9], 0, false, false), completed().first.state())
        assertEquals(SessionState(STOPPED, 1, phases[1], 28, false, false), stopped().first.state())
    }
}
