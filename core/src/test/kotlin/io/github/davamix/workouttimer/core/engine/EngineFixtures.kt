package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.exampleWorkout
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.fail

// Test fixtures for the engine tests.

/** The clock time at which tests start their sessions. Any value works; a non-zero one shows the origin doesn't matter. */
const val T0 = 1_000_000L

/**
 * The timeline of the specification §4.4 example. Phase indexes: 0 get ready (3 s), 1 Jumping jacks (30 s),
 * 2 exercise rest (15 s), 3 Arm circles (10 s), 4 exercise rest (5 s), 5 High knees (40 s), 6 round rest (30 s),
 * 7 Plank (60 s), 8 exercise rest (30 s), 9 Crunches (40 s). Total 263 s.
 */
val exampleTimeline: Timeline = Timeline.of(exampleWorkout)

/**
 * A workout of 61:03 (3663 s) for the accuracy test: 10 rounds, each of five exercises lasting 250, 3, 1, 45 and
 * 2 s, with exercise rests of 20, 0, 0 and 15 s between them. Round rests are 60 s after odd rounds and 0 s after
 * even ones.
 */
val hourLongTimeline: Timeline =
    Timeline.of(
        workout(
            *Array(10) { i ->
                val r = i + 1
                round(
                    "Round $r",
                    exercise("Long", duration = 250, rest = 20, id = "e-$r-long"),
                    exercise("Short", duration = 3, rest = 0, id = "e-$r-short"),
                    exercise("Tiny", duration = 1, rest = 0, id = "e-$r-tiny"),
                    exercise("Mid", duration = 45, rest = 15, id = "e-$r-mid"),
                    exercise("Two", duration = 2, id = "e-$r-two"),
                    rest = if (r % 2 == 1) 60 else 0,
                )
            },
            name = "Hour",
        ),
    )

/** The timeline of a workout made of [rounds], with the `workout` fixture's defaults unless given. */
fun timelineOf(
    vararg rounds: Round,
    defaultExerciseRest: Int = 15,
    defaultRoundRest: Int = 60,
) = Timeline.of(workout(*rounds, defaultExerciseRest = defaultExerciseRest, defaultRoundRest = defaultRoundRest))

/** An event with the clock time of the call that returned it. */
data class Timed(
    val atMillis: Long,
    val event: SessionEvent,
)

/**
 * A compact description of an event: `start 3` (phase 3 started), `countdown`, `3:29` (phase 3 shows 29 s),
 * `round-end 1`, `paused`, `resumed`, `completed` or `stopped`.
 */
fun SessionEvent.code(): String =
    when (this) {
        is SessionEvent.PhaseStarted -> "start $phaseIndex"
        SessionEvent.CountdownStarted -> "countdown"
        is SessionEvent.SecondChanged -> "$phaseIndex:$remainingSeconds"
        is SessionEvent.RoundEnded -> "round-end $roundNumber"
        SessionEvent.Paused -> "paused"
        SessionEvent.Resumed -> "resumed"
        SessionEvent.Completed -> "completed"
        SessionEvent.Stopped -> "stopped"
    }

/** Each event as `<milliseconds after [start]> <code>`, e.g. `3000 start 1`. */
fun List<Timed>.codes(start: Long = T0): List<String> = map { "${it.atMillis - start} ${it.event.code()}" }

/** The events' codes, without times. */
fun List<SessionEvent>.eventCodes(): List<String> = map { it.code() }

/** Calls [SessionMachine.advanceTo] at exactly each [SessionMachine.nextWakeAt] until there is none. */
internal fun SessionMachine.runOnTime(): List<Timed> = runOnTimeUntil(Long.MAX_VALUE)

/** Calls [SessionMachine.advanceTo] at exactly each [SessionMachine.nextWakeAt] up to [limit], included. */
internal fun SessionMachine.runOnTimeUntil(limit: Long): List<Timed> =
    buildList {
        repeat(MAX_WAKE_UPS) {
            val wake = nextWakeAt()?.takeIf { it <= limit } ?: return@buildList
            advanceTo(wake).forEach { add(Timed(wake, it)) }
        }
        fail<Unit>("Still running after $MAX_WAKE_UPS wake-ups")
    }

/**
 * The clock time each phase of [exampleTimeline] starts at when it runs on time from [T0]: 0, 3, 33, 48, 58, 63,
 * 103, 133, 193 and 223 s.
 */
val exampleStarts: List<Long> = listOf(0, 3, 33, 48, 58, 63, 103, 133, 193, 223).map { T0 + it * 1000L }

/** Starts the machine at [at], then runs it on time to the end. */
internal fun SessionMachine.startAndRunOnTime(at: Long = T0): List<Timed> = start(at).map { Timed(at, it) } + runOnTime()

/** More wake-ups than any test workout needs: a 99:59 phase has 5999. */
private const val MAX_WAKE_UPS = 100_000
