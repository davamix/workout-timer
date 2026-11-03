package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.CountdownStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.RoundEnded
import io.github.davamix.workouttimer.core.engine.SessionEvent.SecondChanged
import io.github.davamix.workouttimer.core.engine.SessionStatus.COMPLETED
import io.github.davamix.workouttimer.core.engine.SessionStatus.NOT_STARTED
import io.github.davamix.workouttimer.core.engine.SessionStatus.RUNNING
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.RestKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Running a timeline with time alone: start, then every §4.3 transition (EX-1, EX-2, EX-3, EX-6, EX-7). */
class SessionRunTest {
    private val phases = exampleTimeline.phases

    // Start

    @Test
    fun `before start, the session is in get ready with 3 s and nothing happens on its own`() {
        val machine = SessionMachine(exampleTimeline)

        assertEquals(SessionState(NOT_STARTED, 0, phases[0], 3, false, false), machine.state())
        assertNull(machine.nextWakeAt())
        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(T0 + 10_000))
        assertEquals(NOT_STARTED, machine.state().status)
    }

    @Test
    fun `start runs get ready with 3 s left and starts its countdown`() {
        val machine = SessionMachine(exampleTimeline)

        assertEquals(listOf(PhaseStarted(0, phases[0]), CountdownStarted), machine.start(T0))
        assertEquals(SessionState(RUNNING, 0, phases[0], 3, false, false), machine.state())
        assertEquals(T0 + 1000, machine.nextWakeAt())
    }

    @Test
    fun `get ready counts down for 3 s, then the first exercise starts`() {
        val run = SessionMachine(exampleTimeline).startAndRunOnTime()

        assertEquals(
            listOf("0 start 0", "0 countdown", "1000 0:2", "2000 0:1", "3000 start 1"),
            run.codes().take(5),
        )
        assertEquals(PhaseStarted(1, phases[1]), run[4].event)
        assertInstanceOf(Phase.Exercise::class.java, phases[1])
    }

    // EX-2: exercises count down, then the next phase starts

    @Test
    fun `an exercise counts down every second, and the next phase starts when it reaches zero`() {
        val timeline = timelineOf(round("R", exercise("A", duration = 5), exercise("B", duration = 2)), defaultExerciseRest = 0)

        assertEquals(
            listOf(
                "0 start 0",
                "0 countdown",
                "1000 0:2",
                "2000 0:1",
                "3000 start 1",
                "4000 1:4",
                "5000 1:3",
                "6000 1:2",
                "7000 1:1",
                "8000 start 2",
                "9000 2:1",
                "10000 completed",
            ),
            SessionMachine(timeline).startAndRunOnTime().codes(),
        )
    }

    @Test
    fun `an exercise of 1 s shows no second change, and one of 3 s starts at 3`() {
        val timeline =
            timelineOf(round("R", exercise("One", duration = 1), exercise("Three", duration = 3)), defaultExerciseRest = 0)
        val machine = SessionMachine(timeline)
        machine.start(T0)
        machine.advanceTo(T0 + 3000)

        assertEquals(1, machine.state().remainingSeconds)
        assertEquals(listOf("start 2"), machine.advanceTo(T0 + 4000).eventCodes())
        assertEquals(3, machine.state().remainingSeconds)
        assertEquals(
            listOf("5000 2:2", "6000 2:1", "7000 completed"),
            machine.runOnTime().codes(),
        )
    }

    // EX-3 and §4.3 rules 3 and 6: rests between exercises

    @Test
    fun `the exercise rest runs between two exercises of a round`() {
        val timeline = timelineOf(round("R", exercise("A", duration = 2, rest = 1), exercise("B", duration = 2)))

        assertEquals(
            listOf(
                "0 start 0",
                "0 countdown",
                "1000 0:2",
                "2000 0:1",
                "3000 start 1",
                "4000 1:1",
                "5000 start 2",
                "6000 start 3",
                "7000 3:1",
                "8000 completed",
            ),
            SessionMachine(timeline).startAndRunOnTime().codes(),
        )
        assertEquals(RestKind.EXERCISE, (timeline.phases[2] as Phase.Rest).kind)
    }

    @Test
    fun `a 0 s exercise rest goes straight to the next exercise`() {
        val timeline = timelineOf(round("R", exercise("A", duration = 2), exercise("B", duration = 2)), defaultExerciseRest = 0)

        assertEquals(
            listOf("3000 start 1", "4000 1:1", "5000 start 2", "6000 2:1", "7000 completed"),
            SessionMachine(timeline).startAndRunOnTime().codes().drop(4),
        )
    }

    // §4.3 rule 4: the end of a round

    @Test
    fun `the last exercise of a round sends round-end, then the round rest runs before the next round`() {
        val timeline =
            timelineOf(
                round("R1", exercise("A", duration = 2), rest = 2),
                round("R2", exercise("B", duration = 2)),
            )

        assertEquals(
            listOf(
                "3000 start 1",
                "4000 1:1",
                "5000 round-end 1",
                "5000 start 2",
                "6000 2:1",
                "7000 start 3",
                "8000 3:1",
                "9000 completed",
            ),
            SessionMachine(timeline).startAndRunOnTime().codes().drop(4),
        )
        assertEquals(RestKind.ROUND, (timeline.phases[2] as Phase.Rest).kind)
    }

    @Test
    fun `with a 0 s round rest, round-end and the next exercise come together`() {
        val timeline =
            timelineOf(
                round("R1", exercise("A", duration = 2), rest = 0),
                round("R2", exercise("B", duration = 2)),
            )

        assertEquals(
            listOf("3000 start 1", "4000 1:1", "5000 round-end 1", "5000 start 2", "6000 2:1", "7000 completed"),
            SessionMachine(timeline).startAndRunOnTime().codes().drop(4),
        )
        assertEquals(RoundEnded(1), SessionMachine(timeline).startAndRunOnTime()[6].event)
    }

    // EX-6 and §4.3 rule 5: the end of the workout

    @Test
    fun `the last exercise of the workout completes it, with no round-end`() {
        val run = SessionMachine(exampleTimeline).startAndRunOnTime()

        assertEquals(Completed, run.last().event)
        assertEquals(listOf(RoundEnded(1)), run.map { it.event }.filterIsInstance<RoundEnded>())
    }

    @Test
    fun `a single-exercise workout runs get ready, then the exercise, then completes`() {
        val timeline = timelineOf(round("R", exercise("A", duration = 2)))

        assertEquals(
            listOf("0 start 0", "0 countdown", "1000 0:2", "2000 0:1", "3000 start 1", "4000 1:1", "5000 completed"),
            SessionMachine(timeline).startAndRunOnTime().codes(),
        )
    }

    @Test
    fun `once completed, the state shows the last phase with 0 s, and nothing more happens`() {
        val machine = SessionMachine(exampleTimeline)
        machine.startAndRunOnTime()

        assertEquals(SessionState(COMPLETED, 9, phases[9], 0, false, false), machine.state())
        assertNull(machine.nextWakeAt())
        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(T0 + 1_000_000))
    }

    // UI-3: the displayed time rounds up

    @Test
    fun `remaining seconds round up, change at whole seconds, and show 1 during the last second`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        val jumpingJacksStart = T0 + 3000
        // (milliseconds into Jumping jacks, phase index, remaining seconds)
        val checks =
            listOf(
                Triple(0L, 1, 30),
                Triple(1L, 1, 30),
                Triple(999L, 1, 30),
                Triple(1000L, 1, 29),
                Triple(26_999L, 1, 4),
                Triple(27_000L, 1, 3),
                Triple(28_999L, 1, 2),
                Triple(29_000L, 1, 1),
                Triple(29_999L, 1, 1),
                Triple(30_000L, 2, 15),
            )

        for ((offset, index, seconds) in checks) {
            machine.advanceTo(jumpingJacksStart + offset)
            val state = machine.state()
            assertEquals(index to seconds, state.phaseIndex to state.remainingSeconds, "at $offset ms")
        }
    }

    // The §4.4 example from start to completion

    @Test
    fun `the specification example sends every event on time and completes at 263 s`() {
        val starts = listOf(0, 3, 33, 48, 58, 63, 103, 133, 193, 223)
        val durations = listOf(3, 30, 15, 10, 5, 40, 30, 60, 30, 40)
        val expected =
            buildList {
                for (i in starts.indices) {
                    val start = T0 + starts[i] * 1000L
                    if (i == 6) add(Timed(start, RoundEnded(1)))
                    add(Timed(start, PhaseStarted(i, phases[i])))
                    if (i == 0) add(Timed(start, CountdownStarted))
                    for (s in durations[i] - 1 downTo 1) {
                        add(Timed(start + (durations[i] - s) * 1000L, SecondChanged(i, phases[i], s)))
                    }
                }
                add(Timed(T0 + 263_000, Completed))
            }

        assertEquals(expected, SessionMachine(exampleTimeline).startAndRunOnTime())
        assertEquals(263, exampleTimeline.totalSeconds)
    }

    // Late and early wake-ups

    @Test
    fun `a late wake-up catches up every boundary it passed, in order, and only the current second`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)

        assertEquals(listOf("start 1", "start 2", "start 3", "3:8"), machine.advanceTo(T0 + 50_500).eventCodes())
        assertEquals(T0 + 51_000, machine.nextWakeAt())
        assertEquals(
            SessionMachine(exampleTimeline).startAndRunOnTime().filter { it.atMillis >= T0 + 51_000 },
            machine.runOnTime(),
        )
    }

    @Test
    fun `a late wake-up across a round end sends round-end before the next phase`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.advanceTo(T0 + 100_000)

        // The round rest started at 103 s; at 104.2 s it has 28.8 s left, shown as 29.
        assertEquals(listOf("round-end 1", "start 6", "6:29"), machine.advanceTo(T0 + 104_200).eventCodes())
    }

    @Test
    fun `a late wake-up past the end completes the workout`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)

        assertEquals(Completed, machine.advanceTo(T0 + 500_000).last())
        assertEquals(COMPLETED, machine.state().status)
    }

    @Test
    fun `an early wake-up changes nothing`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)

        assertEquals(emptyList<SessionEvent>(), machine.advanceTo(T0 + 999))
        assertEquals(3, machine.state().remainingSeconds)
        assertEquals(T0 + 1000, machine.nextWakeAt())
    }

    @Test
    fun `the next wake-up is the next whole second of the phase, including its end`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.advanceTo(T0 + 2_000)
        assertEquals(T0 + 3_000, machine.nextWakeAt()) // the end of get ready

        machine.advanceTo(T0 + 3_400)
        assertEquals(T0 + 4_000, machine.nextWakeAt())
    }
}
