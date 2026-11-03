package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.timeline.Phase

/** Where a [WorkoutSession] is in its life. */
enum class SessionStatus {
    /** Created; `start()` hasn't been called yet. */
    NOT_STARTED,

    /** Time is running in the current phase. */
    RUNNING,

    /** Time is stopped in the current phase (EX-4, EX-5). Next, Previous and Stop are available (EX-8, EX-9). */
    PAUSED,

    /** The last exercise ended, or Next was pressed on it (EX-6, EX-9). Final. */
    COMPLETED,

    /** `stop()` was called (EX-8). Final. */
    STOPPED,
}

/**
 * What the runner screen shows (plan D-7). [WorkoutSession.state] publishes a new value after every change: while
 * running, at least once per displayed second.
 *
 * @property status Where the session is in its life.
 * @property phaseIndex Index of the current phase in the session's `timeline.phases`.
 * @property phase The current phase, `timeline.phases[phaseIndex]`.
 * @property remainingSeconds Time left in the current phase, rounded up to the whole second (UI-3): the phase's
 *   duration at its start, and 1 during its last second. 0 once [SessionStatus.COMPLETED].
 * @property awaitingAnnouncement Whether the session is running in get ready and waiting for "Get ready in" to be
 *   spoken before the countdown starts (TTS-10). [remainingSeconds] stays at 3 meanwhile.
 * @property canGoPrevious Whether `previous()` would move: the session is paused and not in get ready (EX-9).
 */
data class SessionState(
    val status: SessionStatus,
    val phaseIndex: Int,
    val phase: Phase,
    val remainingSeconds: Int,
    val awaitingAnnouncement: Boolean,
    val canGoPrevious: Boolean,
)
