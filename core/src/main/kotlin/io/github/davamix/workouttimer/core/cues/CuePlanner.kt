package io.github.davamix.workouttimer.core.cues

import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.timeline.GET_READY_SECONDS
import io.github.davamix.workouttimer.core.timeline.Phase

/** Exercises lasting longer than this have their name spoken when they start (TTS-1). The rule is strict. */
const val ANNOUNCE_NAME_ABOVE_SECONDS = 10

/** The countdown speaks the last this many seconds of an exercise and of get ready (TTS-2, TTS-10). */
const val COUNTDOWN_SECONDS = 3

/**
 * Turns a session's events into cues (plan §2.3 step 5). Pure and stateless: the cues for an event depend only on
 * the event and [voiceCuesEnabled].
 *
 * With voice cues on:
 *
 * - get ready speaks "Get ready in" when it starts, then "three" when its countdown starts (TTS-10);
 * - an exercise lasting more than [ANNOUNCE_NAME_ABOVE_SECONDS] speaks its name when it starts (TTS-1);
 * - get ready and exercises speak "three", "two" and "one" as their displayed time becomes 3, 2 and 1. An exercise
 *   of 3 s or less speaks its first number when it starts, as the engine sends no event for a phase's first
 *   second (TTS-2);
 * - rests are silent (TTS-12);
 * - a round's end plays the round-end sound; the engine only reports it when another round follows (SND-1);
 * - pausing and stopping stop speech, and resuming replays nothing (TTS-3);
 * - completion speaks "Workout complete. Well done!" (TTS-7).
 *
 * @param voiceCuesEnabled The voice setting (TTS-9), read once when the workout starts. When `false`, no
 *   [Cue.Speak] is given; everything else is the same.
 */
class CuePlanner(
    val voiceCuesEnabled: Boolean,
) {
    /** The cues for [event], in the order they must be played. Often empty. */
    fun cuesFor(event: SessionEvent): List<Cue> {
        val cues = allCuesFor(event)
        return if (voiceCuesEnabled) cues else cues.filterNot { it is Cue.Speak }
    }

    private fun allCuesFor(event: SessionEvent): List<Cue> =
        when (event) {
            is SessionEvent.PhaseStarted -> startOf(event.phase)
            SessionEvent.CountdownStarted -> countdownAt(GET_READY_SECONDS)
            is SessionEvent.SecondChanged -> if (event.phase is Phase.Rest) NONE else countdownAt(event.remainingSeconds)
            is SessionEvent.RoundEnded -> listOf(Cue.PlayRoundEndSound)
            SessionEvent.Paused, SessionEvent.Stopped -> listOf(Cue.StopSpeech)
            SessionEvent.Resumed -> NONE
            SessionEvent.Completed -> speak(Utterance.WorkoutComplete)
        }

    private fun startOf(phase: Phase): List<Cue> =
        when (phase) {
            // The countdown starts with CountdownStarted, after the announcement.
            is Phase.GetReady -> speak(Utterance.GetReadyIn)
            is Phase.Exercise ->
                if (phase.durationSeconds > ANNOUNCE_NAME_ABOVE_SECONDS) {
                    speak(Utterance.ExerciseName(phase.exercise.name))
                } else {
                    countdownAt(phase.durationSeconds)
                }
            is Phase.Rest -> NONE
        }

    /** The countdown word for a displayed time of [seconds], if it's one. */
    private fun countdownAt(seconds: Int): List<Cue> = if (seconds in 1..COUNTDOWN_SECONDS) speak(Utterance.Count(seconds)) else NONE

    private fun speak(utterance: Utterance): List<Cue> = listOf(Cue.Speak(utterance))

    private companion object {
        val NONE: List<Cue> = emptyList()
    }
}
