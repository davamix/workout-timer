package io.github.davamix.workouttimer.core.engine

import io.github.davamix.workouttimer.core.timeline.Phase

/**
 * Something that happened in a [WorkoutSession], for the audio (plan D-7). [WorkoutSession.events] delivers them in
 * order, at the moment they happen.
 */
sealed interface SessionEvent {
    /**
     * A phase began running from its start: at `start()`, when the phase before it ended, or on the first `resume()`
     * after `next()` or `previous()` moved to it. Its displayed time is its full duration (TTS-1, TTS-2).
     */
    data class PhaseStarted(
        val phaseIndex: Int,
        val phase: Phase,
    ) : SessionEvent

    /**
     * The get-ready countdown began, with the display at 3 (TTS-10): after "Get ready in" was spoken, at the
     * announcement timeout, on resume, or right after `start()` when the session doesn't wait for the announcement.
     */
    data object CountdownStarted : SessionEvent

    /**
     * The running phase's displayed time changed to [remainingSeconds], which is at least 1 (UI-3, TTS-2). It isn't
     * sent for a phase's first second: [PhaseStarted] covers that. After a wake-up more than a second late, only the
     * current value is sent.
     */
    data class SecondChanged(
        val phaseIndex: Int,
        val phase: Phase,
        val remainingSeconds: Int,
    ) : SessionEvent

    /**
     * The last exercise of round [roundNumber] (1-based) ended on time, and another round follows (§4.3 rule 4,
     * SND-1). Sent just before the next phase's [PhaseStarted]. Moving with Next or Previous never sends it.
     */
    data class RoundEnded(
        val roundNumber: Int,
    ) : SessionEvent

    /** The session paused (EX-4, TTS-3). */
    data object Paused : SessionEvent

    /** The session resumed from where it paused. Nothing that was sent before the pause is sent again (TTS-3). */
    data object Resumed : SessionEvent

    /** The workout ended: its last exercise ended, or Next was pressed on it (EX-6, TTS-7). The last event. */
    data object Completed : SessionEvent

    /** The workout was stopped (EX-8). The last event. */
    data object Stopped : SessionEvent
}
