package io.github.davamix.workouttimer.audio

import io.github.davamix.workouttimer.core.cues.Cue
import io.github.davamix.workouttimer.core.cues.CuePlanner
import io.github.davamix.workouttimer.core.cues.Utterance
import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.engine.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** If the chime's `onDone` doesn't come within this time, speech waiting for it goes ahead anyway. */
const val ROUND_END_SOUND_TIMEOUT_MILLIS = 2_000L

/**
 * Plays a workout's cues (plan §2.3 step 6): a [CuePlanner] decides them from the session's events, and this plays
 * them through the [speaker] and the [soundPlayer]. One workout at a time.
 *
 * - Speech that comes with the round-end sound, or while it plays, waits until it has finished (SND-1).
 * - Pausing and stopping stop speech, and drop speech still waiting for the sound (TTS-3).
 * - When "Get ready in" has been spoken, or can't be, it calls `announcementFinished()` (TTS-10).
 *
 * How the runner uses it:
 *
 * ```
 * val voiceOn = settingsRepository.settings.first().voiceCuesEnabled   // read once (TTS-9)
 * cuePlayer.prepare(voiceOn)
 * val session = WorkoutSession(timeline, clock, viewModelScope, waitForAnnouncement = voiceOn)
 * viewModelScope.launch { cuePlayer.play(session, voiceOn) }
 * session.start()
 * ```
 *
 * @param textOf The text spoken for each utterance: `SpokenText::of` in the app.
 */
class CuePlayer(
    private val speaker: Speaker,
    private val soundPlayer: SoundPlayer,
    private val textOf: (Utterance) -> String,
    private val roundEndSoundTimeoutMillis: Long = ROUND_END_SOUND_TIMEOUT_MILLIS,
) {
    /**
     * Gets the audio ready for a workout: loads the chime and, if [voiceCuesEnabled], prepares the speaker. Call it
     * before `session.start()`. It usually returns at once, as `AppContainer.prepareAudio()` already ran when the app
     * opened. Within about 10 s of a cold start, it can wait up to [SPEAKER_PREPARE_TIMEOUT_MILLIS] for the TTS
     * engine. Returns whether voice cues can be heard: `false` when they're off or the speaker isn't READY.
     */
    suspend fun prepare(voiceCuesEnabled: Boolean): Boolean {
        soundPlayer.prepare()
        return voiceCuesEnabled && speaker.prepare() == SpeakerStatus.READY
    }

    /**
     * Plays [session]'s cues until its events end. It must be the only collector of `session.events`. Speech
     * already handed to the speaker when it returns, or when it's cancelled, carries on, so "Workout complete. Well
     * done!" isn't cut off when the runner leaves.
     *
     * @param voiceCuesEnabled The voice setting, read once when the workout starts (TTS-9). Pass the session's
     *   `waitForAnnouncement` value.
     * @param onCues Called with each event and its cues, before they're played. Only the debug screen uses it.
     */
    suspend fun play(
        session: WorkoutSession,
        voiceCuesEnabled: Boolean,
        onCues: (SessionEvent, List<Cue>) -> Unit = { _, _ -> },
    ) = play(session.events, voiceCuesEnabled, session::announcementFinished, onCues)

    /** [play] on any [events], reporting the end of "Get ready in" to [announcementFinished]. For tests. */
    internal suspend fun play(
        events: Flow<SessionEvent>,
        voiceCuesEnabled: Boolean,
        announcementFinished: () -> Unit,
        onCues: (SessionEvent, List<Cue>) -> Unit = { _, _ -> },
    ) = coroutineScope {
        val planner = CuePlanner(voiceCuesEnabled)
        // The events and the player's own signals, handled one at a time in this coroutine, so nothing needs a lock.
        val inbox = Channel<Input>(Channel.UNLIMITED)
        val forwarder =
            launch {
                events.collect { inbox.send(Input.Event(it)) }
                inbox.send(Input.EventsEnded)
            }

        /** Speech waiting for the round-end sound to finish. */
        val waiting = ArrayDeque<Pending>()
        var soundPlaying = false
        var soundPlay = 0
        var soundTimeout: Job? = null

        fun speak(pending: Pending) {
            if (soundPlaying) waiting.addLast(pending) else speaker.speak(pending.text, pending.onDone)
        }

        fun playCue(cue: Cue) {
            when (cue) {
                is Cue.Speak ->
                    if (cue.utterance == Utterance.GetReadyIn) {
                        if (speaker.status.value == SpeakerStatus.READY) {
                            // Called on a TTS thread: the session takes commands from any thread.
                            speak(Pending(textOf(cue.utterance), announcementFinished))
                        } else {
                            announcementFinished()
                        }
                    } else {
                        speak(Pending(textOf(cue.utterance)))
                    }
                Cue.PlayRoundEndSound -> {
                    val thisPlay = ++soundPlay
                    soundPlaying = true
                    soundTimeout?.cancel()
                    soundTimeout =
                        launch {
                            delay(roundEndSoundTimeoutMillis)
                            inbox.send(Input.SoundFinished(thisPlay))
                        }
                    soundPlayer.playRoundEnd { inbox.trySend(Input.SoundFinished(thisPlay)) }
                }
                Cue.StopSpeech -> {
                    // With voice cues off the speaker is never used, so there's nothing to stop.
                    if (voiceCuesEnabled) speaker.stop()
                    waiting.clear()
                }
            }
        }

        try {
            for (input in inbox) {
                when (input) {
                    is Input.Event -> {
                        val cues = planner.cuesFor(input.event)
                        onCues(input.event, cues)
                        cues.forEach(::playCue)
                    }
                    is Input.SoundFinished ->
                        if (input.play == soundPlay && soundPlaying) {
                            soundPlaying = false
                            soundTimeout?.cancel()
                            while (waiting.isNotEmpty()) speak(waiting.removeFirst())
                        }
                    Input.EventsEnded -> break
                }
            }
        } finally {
            // Speech waiting for the sound is dropped; speech handed to the speaker carries on.
            soundTimeout?.cancel()
            forwarder.cancel()
            inbox.close()
        }
    }

    private class Pending(
        val text: String,
        val onDone: () -> Unit = {},
    )

    private sealed interface Input {
        class Event(
            val event: SessionEvent,
        ) : Input

        /** The round-end sound of play number [play] finished, or its timeout passed. */
        class SoundFinished(
            val play: Int,
        ) : Input

        data object EventsEnded : Input
    }
}
