package io.github.davamix.workouttimer.audio

import kotlinx.coroutines.flow.StateFlow

/** How long [Speaker.prepare] waits at most for the TTS engine. */
const val SPEAKER_PREPARE_TIMEOUT_MILLIS = 5_000L

/** Whether a [Speaker] can be heard. */
enum class SpeakerStatus {
    /** Not connected yet, or connecting. */
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
     * Gets ready to speak: connects to the default TTS engine and selects the voice, unless already READY. When
     * UNAVAILABLE, it starts again from scratch. Returns the status once READY or UNAVAILABLE, or after
     * [timeoutMillis], when it may still be INITIALIZING.
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
