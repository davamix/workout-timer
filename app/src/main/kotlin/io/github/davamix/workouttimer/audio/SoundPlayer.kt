package io.github.davamix.workouttimer.audio

/** Plays the round-end chime (SND-1), the app's only sound effect (SND-2). Safe to call from any thread. */
interface SoundPlayer {
    /** Loads the sound, if it isn't loaded yet, so the first play starts without delay. */
    fun prepare()

    /**
     * Plays the chime from its start, cutting off one still playing, which then counts as finished. [onDone] is
     * called once, on any thread, when the chime has finished or couldn't play; in the second case, at once.
     */
    fun playRoundEnd(onDone: () -> Unit = {})
}
