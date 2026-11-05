package io.github.davamix.workouttimer.ui.runner

import io.github.davamix.workouttimer.core.engine.SessionState
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.timeline.ExerciseInfo
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.RestKind

/** The look of a phase, each with its own background (UI-7). Paused is [RunnerView.paused], on top of it. */
enum class PhaseKind { GET_READY, EXERCISE, REST }

/**
 * What the runner shows for one session state (P7 SPEC §4.3.2). [toRunnerView] builds it from the session's state,
 * so the screen never counts time itself.
 *
 * @property kind The look of the current phase.
 * @property paused Whether the workout is paused: the paused look, "PAUSED" and the paused buttons (UI-11).
 * @property remainingSeconds Time left in the phase, already rounded up by the session (UI-3). Show it with
 *   [formatPhaseTime].
 * @property round The header's round (UI-1, UI-6). During a rest, the round of the exercise before it: for a round
 *   rest, the round that just ended (P7 Q4).
 * @property exercise During an exercise: its name and its position in the round (UI-2, UI-6). Otherwise `null`.
 * @property next During get ready and rests: the exercise that comes next (EX-7, UI-5). Otherwise `null`.
 * @property nextRound During a round rest: the round that starts after it (UI-5). Otherwise `null`.
 * @property canGoPrevious Whether Previous is enabled: `false` in get ready, where it isn't available (EX-9).
 */
data class RunnerView(
    val kind: PhaseKind,
    val paused: Boolean,
    val remainingSeconds: Int,
    val round: RoundLabel,
    val exercise: ExerciseLabel?,
    val next: NextExercise?,
    val nextRound: RoundLabel?,
    val canGoPrevious: Boolean,
)

/** Round [number] of [count], called [name]: "Round 2/3 · Legs". Numbers start at 1. */
data class RoundLabel(
    val number: Int,
    val count: Int,
    val name: String,
)

/** Exercise [name], number [number] of [count] in its round: "Exercise 2/4". Numbers start at 1. */
data class ExerciseLabel(
    val name: String,
    val number: Int,
    val count: Int,
)

/** The exercise shown after "Next:", with its duration: "Next: Lunges (40 s)". */
data class NextExercise(
    val name: String,
    val durationSeconds: Int,
)

/**
 * What the runner shows for this state (P7 SPEC §4.3.2). Meant for the RUNNING and PAUSED states, the only ones the
 * runner draws; any other status reads as not paused.
 */
fun SessionState.toRunnerView(): RunnerView {
    val paused = status == SessionStatus.PAUSED
    return when (val phase = phase) {
        is Phase.GetReady ->
            RunnerView(
                kind = PhaseKind.GET_READY,
                paused = paused,
                remainingSeconds = remainingSeconds,
                round = phase.first.roundLabel(),
                exercise = null,
                next = phase.first.nextExercise(),
                nextRound = null,
                canGoPrevious = canGoPrevious,
            )
        is Phase.Exercise ->
            RunnerView(
                kind = PhaseKind.EXERCISE,
                paused = paused,
                remainingSeconds = remainingSeconds,
                round = phase.exercise.roundLabel(),
                exercise = ExerciseLabel(phase.exercise.name, phase.exercise.numberInRound, phase.exercise.countInRound),
                next = null,
                nextRound = null,
                canGoPrevious = canGoPrevious,
            )
        is Phase.Rest ->
            RunnerView(
                kind = PhaseKind.REST,
                paused = paused,
                remainingSeconds = remainingSeconds,
                round = phase.previous.roundLabel(),
                exercise = null,
                next = phase.next.nextExercise(),
                nextRound = if (phase.kind == RestKind.ROUND) phase.next.roundLabel() else null,
                canGoPrevious = canGoPrevious,
            )
    }
}

/**
 * A phase's remaining time as `mm:ss` (UI-3): 40 gives "00:40", 1 gives "00:01", 5999 gives "99:59". A phase lasts at
 * most 99:59 (WM-7), so the minutes have two digits; a negative value gives "00:00". Digits and a colon only, so it
 * needs no resource (NF-9).
 */
fun formatPhaseTime(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    return "${(total / SECONDS_PER_MINUTE).twoDigits()}:${(total % SECONDS_PER_MINUTE).twoDigits()}"
}

private fun Int.twoDigits() = toString().padStart(2, '0')

private const val SECONDS_PER_MINUTE = 60

private fun ExerciseInfo.roundLabel() = RoundLabel(roundNumber, roundCount, roundName)

private fun ExerciseInfo.nextExercise() = NextExercise(name, durationSeconds)
