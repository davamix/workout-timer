package io.github.davamix.workouttimer.core.cues

/** Something the audio must do, decided by [CuePlanner] from one session event (plan §2.3 step 5). */
sealed interface Cue {
    /** Speak [utterance], after anything already being spoken. */
    data class Speak(
        val utterance: Utterance,
    ) : Cue

    /** Play the round-end sound (SND-1). Speech that comes with it, or while it plays, waits until it has finished. */
    data object PlayRoundEndSound : Cue

    /** Stop any speech in progress and drop speech that is still waiting (TTS-3). The sound isn't cut. */
    data object StopSpeech : Cue
}

/**
 * What can be spoken. The words themselves are English text in the app's resources, never translated (TTS-11), so
 * `:core` only says which phrase.
 */
sealed interface Utterance {
    /** "Get ready in" (TTS-10). When it has been spoken, the player calls `WorkoutSession.announcementFinished()`. */
    data object GetReadyIn : Utterance

    /**
     * "three", "two" or "one" (TTS-2, TTS-10).
     *
     * @throws IllegalArgumentException if [number] isn't 1 to [COUNTDOWN_SECONDS].
     */
    data class Count(
        val number: Int,
    ) : Utterance {
        init {
            require(number in 1..COUNTDOWN_SECONDS) { "A countdown number is 1 to $COUNTDOWN_SECONDS, was $number" }
        }
    }

    /** An exercise's name, as written in the workout (TTS-1). */
    data class ExerciseName(
        val name: String,
    ) : Utterance

    /** "Workout complete. Well done!" (TTS-7). */
    data object WorkoutComplete : Utterance
}
