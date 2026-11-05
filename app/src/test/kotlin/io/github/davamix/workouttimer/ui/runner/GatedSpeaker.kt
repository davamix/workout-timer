package io.github.davamix.workouttimer.ui.runner

import io.github.davamix.workouttimer.audio.FakeSpeaker
import io.github.davamix.workouttimer.audio.Speaker
import io.github.davamix.workouttimer.audio.SpeakerStatus
import kotlinx.coroutines.CompletableDeferred

/**
 * A [Speaker] whose [prepare] waits until the test calls [release], as the TTS engine can for up to 10 s after a cold
 * start (P5). Everything else goes to [delegate].
 */
class GatedSpeaker(
    private val delegate: FakeSpeaker,
) : Speaker by delegate {
    private val gate = CompletableDeferred<Unit>()

    override suspend fun prepare(timeoutMillis: Long): SpeakerStatus {
        gate.await()
        return delegate.prepare(timeoutMillis)
    }

    /** Lets [prepare] return. */
    fun release() {
        gate.complete(Unit)
    }
}
