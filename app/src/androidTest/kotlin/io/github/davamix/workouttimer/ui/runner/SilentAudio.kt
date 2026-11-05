package io.github.davamix.workouttimer.ui.runner

import io.github.davamix.workouttimer.audio.CuePlayer
import io.github.davamix.workouttimer.audio.SoundPlayer
import io.github.davamix.workouttimer.audio.Speaker
import io.github.davamix.workouttimer.audio.SpeakerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Audio for the runner's instrumented tests: nothing is heard.

/**
 * A [Speaker] that is READY, says nothing, and never finishes what it's given. With voice cues on, get ready then
 * waits for "Get ready in" until the session's 5 s announcement timeout (P4), which holds get ready still while a test
 * acts in it.
 */
class SilentSpeaker : Speaker {
    override val status: StateFlow<SpeakerStatus> = MutableStateFlow(SpeakerStatus.READY)
    override val voiceName: String? = "silent"

    override suspend fun prepare(timeoutMillis: Long): SpeakerStatus = SpeakerStatus.READY

    override fun speak(
        text: String,
        onDone: () -> Unit,
    ) = Unit

    override fun stop() = Unit
}

/** A [SoundPlayer] that plays nothing and finishes at once. */
class SilentSoundPlayer : SoundPlayer {
    override fun prepare() = Unit

    override fun playRoundEnd(onDone: () -> Unit) = onDone()
}

/** A cue player that is never heard. */
fun silentCuePlayer() = CuePlayer(SilentSpeaker(), SilentSoundPlayer(), textOf = { "" })
