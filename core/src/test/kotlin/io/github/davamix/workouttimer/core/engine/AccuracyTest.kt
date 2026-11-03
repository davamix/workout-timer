package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.CountdownStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Paused
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Resumed
import io.github.davamix.workouttimer.core.engine.SessionEvent.RoundEnded
import io.github.davamix.workouttimer.core.engine.SessionStatus.COMPLETED
import io.github.davamix.workouttimer.core.engine.SessionStatus.PAUSED
import io.github.davamix.workouttimer.core.engine.SessionStatus.RUNNING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

/**
 * A workout of over 60 minutes, run with random late and early wake-ups and random pauses, stays exactly on schedule
 * (NF-1). Each run uses a fixed seed, so a failure can be replayed.
 */
class AccuracyTest {
    private val timeline = hourLongTimeline
    private val phases = timeline.phases
    private val totalMillis = timeline.totalSeconds * 1000L

    /** Where each phase ends, in milliseconds of running time from Start. */
    private val phaseEnds: List<Long> = phases.runningFold(0L) { end, phase -> end + phase.durationSeconds * 1000L }.drop(1)

    /** The phase index and remaining seconds the screen should show after [runningMillis] of running time. */
    private fun expectedAt(runningMillis: Long): Pair<Int, Int> {
        val index = phaseEnds.indexOfFirst { runningMillis < it }
        if (index < 0) return phases.lastIndex to 0
        return index to ((phaseEnds[index] - runningMillis + 999) / 1000).toInt()
    }

    /** How late each wake-up is, in milliseconds: negative when early. */
    private fun interface Lateness {
        fun next(random: Random): Long
    }

    private val upTo900Late = Lateness { it.nextLong(0, 900) }

    private val sometimesVeryLateOrEarly =
        Lateness { random ->
            val roll = random.nextInt(100)
            when {
                roll < 5 -> random.nextLong(1_000, 3_500)
                roll < 15 -> -random.nextLong(1, 300)
                else -> random.nextLong(0, 900)
            }
        }

    /** The events of a run, and its total time paused. */
    private class Run(
        val events: List<SessionEvent>,
        val pausedMillis: Long,
        val completedAt: Long,
    )

    /**
     * Runs the workout from [T0], waking the machine at each [SessionMachine.nextWakeAt] plus a random [lateness],
     * and pausing at random. After every call, the state must be where the clock says: the expected state for the
     * running time so far, which excludes the time paused.
     */
    private fun run(
        seed: Long,
        lateness: Lateness,
        pauseChancePercent: Int = 2,
    ): Run {
        val random = Random(seed)
        val machine = SessionMachine(timeline)
        val events = mutableListOf<SessionEvent>()
        var now = T0
        var paused = 0L

        fun check(expectedStatus: SessionStatus) {
            val running = now - T0 - paused
            val state = machine.state()
            val status = if (running >= totalMillis) COMPLETED else expectedStatus
            assertEquals(status, state.status, "seed $seed, running time $running ms")
            assertEquals(expectedAt(running), state.phaseIndex to state.remainingSeconds, "seed $seed, running time $running ms")
        }

        events += machine.start(now)
        check(RUNNING)
        while (true) {
            val wake = machine.nextWakeAt() ?: break
            now = maxOf(now, wake + lateness.next(random))
            events += machine.advanceTo(now)
            check(RUNNING)

            if (machine.state().status == RUNNING && random.nextInt(100) < pauseChancePercent) {
                now += random.nextLong(0, 1_000)
                events += machine.pause(now)
                check(PAUSED)
                if (machine.state().status != PAUSED) break // it completed before the pause
                val length = random.nextLong(1, 30_000)
                now += length
                paused += length
                events += machine.resume(now)
                check(RUNNING)
            }
        }
        assertEquals(COMPLETED, machine.state().status, "seed $seed")
        return Run(events, paused, completedAt = now)
    }

    private val onTimeEvents = SessionMachine(timeline).startAndRunOnTime().map { it.event }

    @Test
    fun `the test workout lasts over 60 minutes and has every kind of phase`() {
        assertEquals(3663, timeline.totalSeconds)
        assertTrue(phases.any { it.durationSeconds == 1 })
        assertEquals(9, onTimeEvents.count { it is RoundEnded })
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = [1, 2, 3, 4, 5])
    fun `with wake-ups up to 900 ms late and random pauses, every event comes, in order, on schedule`(seed: Long) {
        val run = run(seed, upTo900Late)

        assertEquals(onTimeEvents, run.events.filter { it != Paused && it != Resumed })
        assertTrue(run.events.count { it == Paused } > 0, "seed $seed paused at least once")
        assertTrue(run.completedAt - T0 - run.pausedMillis - totalMillis in 0 until 1_000, "seed $seed")
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = [11, 12, 13, 14, 15])
    fun `with some wake-ups over a second late or early, the state always matches the clock`(seed: Long) {
        val run = run(seed, sometimesVeryLateOrEarly)

        fun List<SessionEvent>.milestones() = filter { it is PhaseStarted || it is RoundEnded || it is CountdownStarted || it is Completed }
        assertEquals(onTimeEvents.milestones(), run.events.milestones())
        assertTrue(run.completedAt - T0 - run.pausedMillis - totalMillis in 0 until 3_500, "seed $seed")
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = [21, 22, 23])
    fun `with frequent pauses, the run still completes on schedule`(seed: Long) {
        val run = run(seed, upTo900Late, pauseChancePercent = 30)

        assertEquals(onTimeEvents, run.events.filter { it != Paused && it != Resumed })
        assertTrue(run.events.count { it == Paused } > 100, "seed $seed")
    }
}
