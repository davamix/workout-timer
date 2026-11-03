package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.CountdownStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Paused
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Resumed
import io.github.davamix.workouttimer.core.engine.SessionEvent.Stopped
import io.github.davamix.workouttimer.core.engine.SessionStatus.PAUSED
import io.github.davamix.workouttimer.core.engine.SessionStatus.RUNNING
import io.github.davamix.workouttimer.core.engine.SessionStatus.STOPPED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Get ready waits for "Get ready in" to be spoken, with a timeout (EX-7, TTS-10). */
class GetReadyHandshakeTest {
    private val phases = exampleTimeline.phases

    private fun waitingMachine(timeoutMillis: Long = ANNOUNCEMENT_TIMEOUT_MILLIS) =
        SessionMachine(exampleTimeline, waitForAnnouncement = true, announcementTimeoutMillis = timeoutMillis)

    /** The on-time run of the example without waiting, from the countdown on, shifted to start at [countdownAt]. */
    private fun referenceFromCountdown(countdownAt: Long): List<Timed> =
        SessionMachine(exampleTimeline)
            .startAndRunOnTime()
            .drop(2) // PhaseStarted(0) and CountdownStarted
            .map { it.copy(atMillis = it.atMillis - T0 + countdownAt) }

    @Test
    fun `the default timeout is 5 s`() {
        assertEquals(5_000L, ANNOUNCEMENT_TIMEOUT_MILLIS)
    }

    @Test
    fun `while waiting for the announcement, get ready stays at 3 s`() {
        val machine = waitingMachine()

        assertEquals(listOf(PhaseStarted(0, phases[0])), machine.start(T0))
        assertEquals(SessionState(RUNNING, 0, phases[0], 3, true, false), machine.state())
        assertEquals(T0 + 5_000, machine.nextWakeAt())

        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(T0 + 4_999))
        assertEquals(SessionState(RUNNING, 0, phases[0], 3, true, false), machine.state())
    }

    @Test
    fun `the countdown starts when the announcement finishes, and the run follows from there`() {
        val machine = waitingMachine()
        machine.start(T0)
        machine.advanceTo(T0 + 1_000)

        assertEquals(listOf(CountdownStarted), machine.announcementFinished(T0 + 1_234))
        assertEquals(SessionState(RUNNING, 0, phases[0], 3, false, false), machine.state())
        assertEquals(referenceFromCountdown(T0 + 1_234), machine.runOnTime())
    }

    @Test
    fun `with no announcement, the timeout starts the countdown at exactly its end`() {
        val machine = waitingMachine()
        machine.start(T0)

        assertEquals(listOf(CountdownStarted), machine.advanceTo(T0 + 5_000))
        assertEquals(referenceFromCountdown(T0 + 5_000), machine.runOnTime())
    }

    @Test
    fun `a late wake-up after the timeout keeps the countdown on the timeout's schedule`() {
        val machine = waitingMachine()
        machine.start(T0)

        assertEquals(listOf("countdown", "0:2"), machine.advanceTo(T0 + 6_300).eventCodes())
        assertEquals(T0 + 7_000, machine.nextWakeAt())
    }

    @Test
    fun `the timeout can be set`() {
        val machine = waitingMachine(timeoutMillis = 2_000)
        machine.start(T0)

        assertEquals(T0 + 2_000, machine.nextWakeAt())
        assertEquals(listOf(CountdownStarted), machine.advanceTo(T0 + 2_000))
    }

    // Calls that come at the wrong moment

    @Test
    fun `an announcement that finishes after the timeout is ignored`() {
        val machine = waitingMachine()
        machine.start(T0)
        machine.advanceTo(T0 + 5_500)
        val state = machine.state()

        assertEquals(emptyList<SessionEvent>(), machine.announcementFinished(T0 + 5_600))
        assertEquals(state, machine.state())
        assertEquals(T0 + 6_000, machine.nextWakeAt())
    }

    @Test
    fun `a second announcementFinished is ignored`() {
        val machine = waitingMachine()
        machine.start(T0)
        machine.announcementFinished(T0 + 1_000)

        assertEquals(emptyList<SessionEvent>(), machine.announcementFinished(T0 + 1_500))
        assertEquals(T0 + 2_000, machine.nextWakeAt())
    }

    @Test
    fun `announcementFinished while paused is ignored`() {
        val machine = waitingMachine()
        machine.start(T0)
        machine.pause(T0 + 500)
        val state = machine.state()

        assertEquals(emptyList<SessionEvent>(), machine.announcementFinished(T0 + 700))
        assertEquals(state, machine.state())
    }

    @Test
    fun `announcementFinished before start, or in another phase, is ignored`() {
        val notStarted = waitingMachine()
        assertEquals(emptyList<SessionEvent>(), notStarted.announcementFinished(T0))

        val inExercise = SessionMachine(exampleTimeline)
        inExercise.start(T0)
        inExercise.runOnTimeUntil(T0 + 5_000)
        val state = inExercise.state()
        assertEquals(emptyList<SessionEvent>(), inExercise.announcementFinished(T0 + 5_000))
        assertEquals(state, inExercise.state())
    }

    // Pause, Next and Stop while waiting

    @Test
    fun `pausing while waiting, then resuming, starts the countdown at once`() {
        val machine = waitingMachine()
        machine.start(T0)

        assertEquals(listOf(Paused), machine.pause(T0 + 600))
        assertEquals(SessionState(PAUSED, 0, phases[0], 3, false, false), machine.state())
        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(T0 + 20_000)) // the timeout doesn't run while paused

        assertEquals(listOf(Resumed, CountdownStarted), machine.resume(T0 + 30_000))
        assertEquals(referenceFromCountdown(T0 + 30_000), machine.runOnTime())
    }

    @Test
    fun `a pause during the countdown, after the announcement, resumes normally`() {
        val machine = waitingMachine()
        machine.start(T0)
        machine.announcementFinished(T0 + 1_000)
        machine.pause(T0 + 1_500)

        assertEquals(listOf(Resumed), machine.resume(T0 + 9_000))
        assertEquals(T0 + 9_500, machine.nextWakeAt())
    }

    @Test
    fun `Next while waiting goes to the first exercise, which starts on resume`() {
        val machine = waitingMachine()
        machine.start(T0)
        machine.pause(T0 + 600)

        assertEquals(emptyList<SessionEvent>(), machine.next(T0 + 700))
        assertEquals(SessionState(PAUSED, 1, phases[1], 30, false, true), machine.state())
        assertEquals(listOf(Resumed, PhaseStarted(1, phases[1])), machine.resume(T0 + 1_000))
    }

    @Test
    fun `stop while waiting ends the session`() {
        val machine = waitingMachine()
        machine.start(T0)

        assertEquals(listOf(Stopped), machine.stop(T0 + 600))
        assertEquals(SessionState(STOPPED, 0, phases[0], 3, false, false), machine.state())
    }

    @Test
    fun `without waiting, the countdown starts right after get ready`() {
        assertEquals(listOf(PhaseStarted(0, phases[0]), CountdownStarted), SessionMachine(exampleTimeline).start(T0))
    }
}
