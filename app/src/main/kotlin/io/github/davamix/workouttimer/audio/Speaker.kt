package io.github.davamix.workouttimer.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * How long [Speaker.prepare] waits at most for the TTS engine. From a cold start, connecting and warming up take 9–10 s
 * on the tablet. The app starts preparing when its activity is created, so a workout's call waits for less.
 */
const val SPEAKER_PREPARE_TIMEOUT_MILLIS = 10_000L

/** Whether a [Speaker] can be heard. */
enum class SpeakerStatus {
    /** Not connected yet, connecting, or warming up. */
    INITIALIZING,

    /** Connected, with the English (United States) voice selected. Speech can be heard. */
    READY,

    /** The engine failed, or has no English (United States) voice. [Speaker.prepare] tries again. */
    UNAVAILABLE,
}

/** Speaks text with the English (United States) voice (TTS-11). Safe to call from any thread. */
interface Speaker {
    /** Whether speech can be heard now. */
    val status: StateFlow<SpeakerStatus>

    /** The selected voice's name, for the debug screen and logs. `null` until [SpeakerStatus.READY]. */
    val voiceName: String?

    /**
     * Gets ready to speak: connects to the default TTS engine, selects the voice and warms the engine up, unless
     * that's done or under way. When UNAVAILABLE, it starts again from scratch. Returns the status once READY or
     * UNAVAILABLE, or after [timeoutMillis], when it may still be INITIALIZING and carries on preparing. Several calls
     * may wait at once; they share one preparation.
     */
    suspend fun prepare(timeoutMillis: Long = SPEAKER_PREPARE_TIMEOUT_MILLIS): SpeakerStatus

    /**
     * Speaks [text] after anything already queued. [onDone] is called once, on any thread, when it has been spoken
     * or couldn't be (not READY, engine error), at once in the second case. It isn't called when [stop] cuts the
     * text off.
     */
    fun speak(
        text: String,
        onDone: () -> Unit = {},
    )

    /** Stops speech in progress and drops queued speech. Their `onDone` callbacks are never called. */
    fun stop()
}
