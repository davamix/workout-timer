package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.CountdownStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Paused
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Resumed
import io.github.davamix.workouttimer.core.engine.SessionEvent.RoundEnded
import io.github.davamix.workouttimer.core.engine.SessionEvent.Stopped
import io.github.davamix.workouttimer.core.engine.SessionStatus.COMPLETED
import io.github.davamix.workouttimer.core.engine.SessionStatus.PAUSED
import io.github.davamix.workouttimer.core.engine.SessionStatus.STOPPED
import io.github.davamix.workouttimer.core.timeline.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.random.Random

/** The session on virtual time: its timer, its flows, and commands from other threads and from collectors (D-7, NF-1). */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionTest {
    private val phases = exampleTimeline.phases

    /** The machine's on-time run of the example, with times from 0 like the test scheduler's. */
    private val reference = SessionMachine(exampleTimeline).startAndRunOnTime().map { it.copy(atMillis = it.atMillis - T0) }

    private fun TestScope.session(
        timeline: Timeline = exampleTimeline,
        waitForAnnouncement: Boolean = false,
        sleepFor: (Long) -> Long = { it },
    ) = WorkoutSession(
        timeline,
        Clock { testScheduler.currentTime },
        backgroundScope,
        waitForAnnouncement,
        ANNOUNCEMENT_TIMEOUT_MILLIS,
        sleepFor,
    )

    /** Collects the session's events as they come, each with the virtual time it arrived at. */
    private fun TestScope.record(session: WorkoutSession): List<Timed> {
        val events = mutableListOf<Timed>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            session.events.collect { events += Timed(testScheduler.currentTime, it) }
        }
        return events
    }

    /**
     * Waits until the session completes or stops. While the test waits, virtual time runs the session's timer.
     * (`advanceUntilIdle()` wouldn't: it ignores work that only `backgroundScope` has scheduled.)
     */
    private suspend fun WorkoutSession.awaitEnd() {
        state.first { it.status == COMPLETED || it.status == STOPPED }
    }

    // The timer

    @Test
    fun `the example runs on virtual time and completes at exactly 263 s`() =
        runTest {
            val session = session()
            val events = record(session)

            session.start()
            session.awaitEnd()

            assertEquals(reference, events)
            assertEquals(263_000, currentTime)
            assertEquals(SessionState(COMPLETED, 9, phases[9], 0, false, false), session.state.value)
        }

    @Test
    fun `the state changes once per displayed second`() =
        runTest {
            val session = session()
            val states = mutableListOf<SessionState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.state.collect { states += it } }

            session.start()
            session.awaitEnd()

            assertEquals((30 downTo 1).toList(), states.filter { it.phaseIndex == 1 }.map { it.remainingSeconds })
            assertEquals(phases.sumOf { it.durationSeconds } + 2, states.size) // plus NOT_STARTED and COMPLETED
        }

    @Test
    fun `commands mid-run apply at once, and the timer follows them`() =
        runTest {
            val session = session()
            val events = record(session)
            session.start()
            advanceTimeBy(10_500)

            session.pause()
            assertEquals(SessionState(PAUSED, 1, phases[1], 23, false, true), session.state.value)
            advanceTimeBy(60_000)
            assertEquals(SessionState(PAUSED, 1, phases[1], 23, false, true), session.state.value)

            session.resume()
            session.awaitEnd()
            assertEquals(Timed(323_000, Completed), events.last())
        }

    @Test
    fun `with no announcement, the timer starts the countdown at the timeout`() =
        runTest {
            val session = session(waitForAnnouncement = true)
            val events = record(session)

            session.start()
            session.awaitEnd()

            assertEquals(Timed(5_000, CountdownStarted), events[1])
            assertEquals(268_000, currentTime)
        }

    // The flows

    @Test
    fun `events sent before collection starts are kept, and the flow completes after the workout`() =
        runTest {
            val session = session()
            session.start()
            session.awaitEnd()

            assertEquals(reference.map { it.event }, session.events.toList())
        }

    @Test
    fun `the events flow completes after stop`() =
        runTest {
            val session = session()
            session.start()
            advanceTimeBy(5_500)
            session.pause()
            session.stop()

            assertEquals(listOf(Paused, Stopped), session.events.toList().takeLast(2))
            assertEquals(STOPPED, session.state.value.status)
        }

    @Test
    fun `stop before start ends the session`() =
        runTest {
            val session = session()
            session.stop()

            assertEquals(listOf(Stopped), session.events.toList())
            session.start()
            assertEquals(STOPPED, session.state.value.status)
        }

    @Test
    fun `cancelling the scope stops the timer and completes the events`() =
        runTest {
            val job = Job()
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + job)
            val session = WorkoutSession(exampleTimeline, Clock { testScheduler.currentTime }, scope)
            session.start()
            advanceTimeBy(5_500)
            val state = session.state.value

            job.cancel()
            advanceTimeBy(100_000)

            assertEquals(state, session.state.value)
            assertEquals(reference.filter { it.atMillis < 5_500 }.map { it.event }, session.events.toList())
        }

    // Threads and collectors

    @Test
    fun `a command from another thread is applied`() =
        runTest {
            val session = session(waitForAnnouncement = true)
            session.start()

            thread { session.announcementFinished() }.join()

            assertEquals(false, session.state.value.awaitingAnnouncement)
            session.awaitEnd()
            assertEquals(listOf(PhaseStarted(0, phases[0]), CountdownStarted), session.events.toList().take(2))
            assertEquals(263_000, currentTime)
        }

    @Test
    fun `commands from many threads at once apply one at a time`() =
        runTest {
            val session = session()
            session.start()
            val failures = ConcurrentLinkedQueue<Throwable>()
            val go = CountDownLatch(1)
            val threads =
                List(8) {
                    thread {
                        try {
                            go.await()
                            repeat(500) {
                                session.pause()
                                session.resume()
                            }
                        } catch (t: Throwable) {
                            failures += t
                        }
                    }
                }

            go.countDown()
            threads.forEach { it.join() }
            session.stop()

            assertEquals(emptyList<Throwable>(), failures.toList())
            val events = session.events.toList()
            val pauses = events.filter { it == Paused || it == Resumed }
            assertTrue(pauses.isNotEmpty())
            // A pause only applies while running and a resume only while paused, so they must alternate.
            assertEquals(List(pauses.size) { if (it % 2 == 0) Paused else Resumed }, pauses)
            assertEquals(Stopped, events.last())
        }

    @Test
    fun `a collector that pauses on round-end still sees the next phase start first`() =
        runTest {
            val session = session()
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                session.events.collect { event ->
                    seen += event
                    if (event is RoundEnded) session.pause()
                }
            }

            session.start()
            session.state.first { it.status == PAUSED }

            assertEquals(listOf(RoundEnded(1), PhaseStarted(6, phases[6]), Paused), seen.takeLast(3))
            assertEquals(SessionState(PAUSED, 6, phases[6], 30, false, true), session.state.value)
        }

    @Test
    fun `a collector that answers get ready at once starts the countdown in order`() =
        runTest {
            val session = session(waitForAnnouncement = true)
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                session.events.collect { event ->
                    seen += event
                    if (event is PhaseStarted && event.phaseIndex == 0) session.announcementFinished()
                }
            }

            session.start()
            assertEquals(listOf(PhaseStarted(0, phases[0]), CountdownStarted), seen)

            session.awaitEnd()
            assertEquals(reference.map { it.event }, seen)
        }

    // Late and early wake-ups

    @Test
    fun `with wake-ups up to 900 ms late, every event still comes, each less than 900 ms late`() =
        runTest {
            val random = Random(7)
            val session = session(sleepFor = { it + random.nextLong(0, 900) })
            val events = record(session)

            session.start()
            session.awaitEnd()

            assertEquals(reference.map { it.event }, events.map { it.event })
            val lateness = events.zip(reference) { actual, expected -> actual.atMillis - expected.atMillis }
            assertTrue(lateness.all { it in 0 until 900 }, "lateness between ${lateness.min()} and ${lateness.max()} ms")
        }

    @Test
    fun `with early wake-ups, the timer sleeps again and every event comes on time`() =
        runTest {
            val random = Random(8)
            val session = session(sleepFor = { (it - random.nextLong(0, 50)).coerceAtLeast(1) })
            val events = record(session)

            session.start()
            session.awaitEnd()

            assertEquals(reference, events)
        }
}
