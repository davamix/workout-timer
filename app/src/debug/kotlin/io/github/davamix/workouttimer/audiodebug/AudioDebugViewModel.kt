package io.github.davamix.workouttimer.audiodebug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.davamix.workouttimer.core.cues.Cue
import io.github.davamix.workouttimer.core.cues.Utterance
import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.engine.WorkoutSession
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Runs a debug workout with audio and no runner screen (P5 SPEC §4.3.7). It starts the session as the runner will
 * (SPEC §4.3.6), so it is also a working example of the wiring. It doesn't pause in the background: that's P7's
 * `EX-10`.
 */
class AudioDebugViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val workouts: List<Workout> = debugWorkouts

    private val mutableSelected = MutableStateFlow(0)
    val selected: StateFlow<Int> = mutableSelected.asStateFlow()

    /** The stored voice setting (TTS-9): the switch reads and writes it. */
    val voiceCuesEnabled: StateFlow<Boolean> =
        container.settingsRepository.settings
            .map { it.voiceCuesEnabled }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val speaker = container.speaker

    private val mutablePreparing = MutableStateFlow(false)
    val preparing: StateFlow<Boolean> = mutablePreparing.asStateFlow()

    private val mutableSession = MutableStateFlow<WorkoutSession?>(null)

    /** The workout running or last run, if any. */
    val session: StateFlow<WorkoutSession?> = mutableSession.asStateFlow()

    private val mutableLog = MutableStateFlow<List<String>>(emptyList())

    /** The last [LOG_SIZE] events with their cues, newest first. Ticks with no cue are left out. */
    val log: StateFlow<List<String>> = mutableLog.asStateFlow()

    fun select(index: Int) {
        if (!isActive()) mutableSelected.value = index
    }

    fun setVoiceCuesEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setVoiceCuesEnabled(enabled) }
    }

    /** Starts the selected workout, as the runner will (SPEC §4.3.6). */
    fun start() {
        if (isActive() || mutablePreparing.value) return
        viewModelScope.launch {
            mutablePreparing.value = true
            val voiceOn =
                container.settingsRepository.settings
                    .first()
                    .voiceCuesEnabled
            container.cuePlayer.prepare(voiceOn)
            mutablePreparing.value = false

            val session =
                WorkoutSession(
                    Timeline.of(workouts[mutableSelected.value]),
                    container.clock,
                    viewModelScope,
                    waitForAnnouncement = voiceOn,
                )
            mutableLog.value = emptyList()
            mutableSession.value = session
            val startedAt = container.clock.nowMillis()
            launch {
                container.cuePlayer.play(session, voiceOn) { event, cues ->
                    record(container.clock.nowMillis() - startedAt, event, cues)
                }
            }
            session.start()
        }
    }

    fun pause() = mutableSession.value?.pause()

    fun resume() = mutableSession.value?.resume()

    fun next() = mutableSession.value?.next()

    fun previous() = mutableSession.value?.previous()

    fun stop() = mutableSession.value?.stop()

    /** Whether a workout is running or paused, or about to start. */
    private fun isActive(): Boolean {
        val status =
            mutableSession.value
                ?.state
                ?.value
                ?.status ?: return false
        return status != SessionStatus.COMPLETED && status != SessionStatus.STOPPED
    }

    private fun record(
        elapsedMillis: Long,
        event: SessionEvent,
        cues: List<Cue>,
    ) {
        // A tick with no cue would only push the interesting lines out of the log.
        if (event is SessionEvent.SecondChanged && cues.isEmpty()) return
        val time = String.format(Locale.ROOT, "%7.1f s", elapsedMillis / 1000.0)
        val played = if (cues.isEmpty()) "" else "  →  " + cues.joinToString { it.describe() }
        val line = "$time  ${event.describe()}$played"
        mutableLog.update { (listOf(line) + it).take(LOG_SIZE) }
    }

    private companion object {
        const val LOG_SIZE = 50

        fun SessionEvent.describe(): String =
            when (this) {
                is SessionEvent.PhaseStarted -> "start ${phase.describe()}"
                SessionEvent.CountdownStarted -> "countdown"
                is SessionEvent.SecondChanged -> "${phase.describe()} $remainingSeconds"
                is SessionEvent.RoundEnded -> "round $roundNumber ended"
                SessionEvent.Paused -> "paused"
                SessionEvent.Resumed -> "resumed"
                SessionEvent.Completed -> "completed"
                SessionEvent.Stopped -> "stopped"
            }

        fun Phase.describe(): String =
            when (this) {
                is Phase.GetReady -> "get ready"
                is Phase.Exercise -> exercise.name
                is Phase.Rest -> "rest"
            }

        fun Cue.describe(): String =
            when (this) {
                is Cue.Speak ->
                    when (val utterance = utterance) {
                        Utterance.GetReadyIn -> "say \"Get ready in\""
                        is Utterance.Count -> "say ${utterance.number}"
                        is Utterance.ExerciseName -> "say \"${utterance.name}\""
                        Utterance.WorkoutComplete -> "say \"Workout complete\""
                    }
                Cue.PlayRoundEndSound -> "sound"
                Cue.StopSpeech -> "stop speech"
            }
    }
}
