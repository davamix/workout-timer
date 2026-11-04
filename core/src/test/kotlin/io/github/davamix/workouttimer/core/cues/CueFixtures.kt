package io.github.davamix.workouttimer.core.cues

import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.engine.SessionMachine
import io.github.davamix.workouttimer.core.engine.T0
import io.github.davamix.workouttimer.core.engine.Timed
import io.github.davamix.workouttimer.core.engine.runOnTime
import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.timeline.ExerciseInfo
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.core.workout

// Test fixtures for the cue tests. The engine fixtures (T0, exampleTimeline, Timed, runOnTime…) come from P4.

/**
 * The edge-case workout of SPEC §4.3.7, 30 s long. Phase indexes: 0 get ready (3 s), 1 "Ten" (10 s), 2 "Eleven"
 * (11 s), 3 "Three" (3 s), 4 "Two" (2 s), 5 "One" (1 s). Every rest is 0 s, so there are no rest phases, and round
 * "A" ends straight into round "B".
 */
val edgeTimeline: Timeline =
    Timeline.of(
        workout(
            round("A", exercise("Ten", duration = 10), exercise("Eleven", duration = 11), rest = 0),
            round("B", exercise("Three", duration = 3), exercise("Two", duration = 2), exercise("One", duration = 1)),
            name = "Edge cases",
            defaultExerciseRest = 0,
        ),
    )

/** An exercise phase lasting [duration] s, alone in its round and workout. */
fun exercisePhase(
    name: String,
    duration: Int,
) = Phase.Exercise(exerciseInfo(name, duration))

/** The [ExerciseInfo] of an exercise alone in its round and workout. */
fun exerciseInfo(
    name: String,
    duration: Int,
) = ExerciseInfo(
    exerciseId = "e-$name",
    name = name,
    durationSeconds = duration,
    roundId = "r-R",
    roundName = "R",
    roundNumber = 1,
    roundCount = 1,
    numberInRound = 1,
    countInRound = 1,
)

/**
 * A compact description of a cue: the phrase for speech (`Get ready in`, `three`, `Plank`, `Workout complete`),
 * `sound` for the round-end sound, or `stop` for stopping speech.
 */
fun Cue.code(): String =
    when (this) {
        is Cue.Speak ->
            when (val utterance = utterance) {
                Utterance.GetReadyIn -> "Get ready in"
                is Utterance.Count -> listOf("one", "two", "three")[utterance.number - 1]
                is Utterance.ExerciseName -> utterance.name
                Utterance.WorkoutComplete -> "Workout complete"
            }
        Cue.PlayRoundEndSound -> "sound"
        Cue.StopSpeech -> "stop"
    }

/** The cues [planner] gives for these events, each as `<milliseconds after [start]> <code>`, e.g. `4500 Plank`. */
fun List<Timed>.cueCodes(
    planner: CuePlanner,
    start: Long = T0,
): List<String> = flatMap { timed -> planner.cuesFor(timed.event).map { "${timed.atMillis - start} ${it.code()}" } }

/** The cues for [event], as codes without times. */
fun CuePlanner.codesFor(event: SessionEvent): List<String> = cuesFor(event).map { it.code() }

/** Calls [command] at [now] and returns its events with that time. */
internal fun SessionMachine.at(
    now: Long,
    command: SessionMachine.(Long) -> List<SessionEvent>,
): List<Timed> = command(now).map { Timed(now, it) }

/**
 * Runs a machine that waits for the announcement: starts it at [T0], reports "Get ready in" finished
 * [announcementMillis] later, then runs it on time to the end.
 */
internal fun runWithAnnouncement(
    timeline: Timeline,
    announcementMillis: Long,
): List<Timed> {
    val machine = SessionMachine(timeline, waitForAnnouncement = true)
    return machine.at(T0) { start(it) } +
        machine.at(T0 + announcementMillis) { announcementFinished(it) } +
        machine.runOnTime()
}
