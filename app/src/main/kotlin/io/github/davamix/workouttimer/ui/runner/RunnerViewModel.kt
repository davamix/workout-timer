package io.github.davamix.workouttimer.ui.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.audio.CuePlayer
import io.github.davamix.workouttimer.core.engine.Clock
import io.github.davamix.workouttimer.core.engine.SessionState
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.engine.WorkoutSession
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.validate
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.settings.SettingsRepository
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.ui.completed.WorkoutSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The workout runner (P7 SPEC §4.3): it owns the workout's [WorkoutSession] (plan D-7), plays its cues through the
 * [cuePlayer], and turns the session's state into what the screen shows.
 *
 * When it's created, it reads and validates the workout, reads the voice setting once (TTS-9), prepares the audio,
 * and starts the session once the screen is in the foreground (§4.3.1). A runner that Android rebuilt after closing
 * the app has no session, and exits at once (EX-11, §4.3.5).
 *
 * @param workoutId The saved workout to run.
 * @param clock The monotonic clock the session runs on (plan D-6).
 * @param cuePlayer Plays the voice and sound cues (P5). The only collector of the session's events.
 * @param savedStateHandle The navigation entry's saved state, which keeps the `started` flag.
 * @param audioDispatcher Where `cuePlayer.prepare()` runs, off the main thread: it loads the chime and connects the
 *   TTS engine. Tests pass their own.
 */
class RunnerViewModel(
    private val workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val cuePlayer: CuePlayer,
    savedStateHandle: SavedStateHandle,
    private val audioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(),
    RunnerActions {
    private val mutableState = MutableStateFlow<RunnerState>(RunnerState.Starting())

    /** What the runner shows, or the outcome it has finished with. */
    val state: StateFlow<RunnerState> = mutableState.asStateFlow()

    /** The running workout, once started. */
    private var session: WorkoutSession? = null

    /** Reads the workout, prepares the audio and starts the session. Cancelled if the runner is left meanwhile. */
    private var startJob: Job? = null

    /** Whether the screen is in the foreground. A workout still starting waits for it (EX-10). */
    private val inForeground = MutableStateFlow(true)

    init {
        if (savedStateHandle.get<Boolean>(STARTED_KEY) == true) {
            mutableState.value = RunnerState.Exited
        } else {
            savedStateHandle[STARTED_KEY] = true
            startJob = viewModelScope.launch { open() }
        }
    }

    /** §4.3.1, steps 3 to 7. */
    private suspend fun open() {
        val workout = workoutRepository.get(workoutId)
        if (workout == null || workout.validate().isNotEmpty()) {
            // Timeline.of would throw on an invalid workout (P3 Q2).
            mutableState.value = RunnerState.Unavailable
            return
        }
        mutableState.value = RunnerState.Starting(workout.name)
        val voiceOn = settingsRepository.settings.first().voiceCuesEnabled
        withContext(audioDispatcher) { cuePlayer.prepare(voiceOn) }
        inForeground.first { it }
        start(workout, voiceOn)
    }

    private fun start(
        workout: Workout,
        voiceOn: Boolean,
    ) {
        val timeline = Timeline.of(workout)
        val session = WorkoutSession(timeline, clock, viewModelScope, waitForAnnouncement = voiceOn)
        this.session = session
        val playing = viewModelScope.launch { cuePlayer.play(session, voiceOn) }
        viewModelScope.launch {
            val end =
                session.state
                    .onEach { if (it.status == SessionStatus.RUNNING || it.status == SessionStatus.PAUSED) show(it) }
                    .first { it.status == SessionStatus.COMPLETED || it.status == SessionStatus.STOPPED }
            if (end.status == SessionStatus.COMPLETED) {
                // play() returns once it has handled Completed, so "Workout complete. Well done!" is with the speaker.
                playing.join()
                mutableState.value = RunnerState.Completed(timeline.summary(workout.name))
            } else {
                mutableState.value = RunnerState.Exited
            }
        }
        session.start()
    }

    /** Shows a running or paused [state]. The Stop dialog stays open only while paused. */
    private fun show(state: SessionState) {
        val wasConfirming = (mutableState.value as? RunnerState.Active)?.confirmingStop == true
        mutableState.value =
            RunnerState.Active(
                view = state.toRunnerView(),
                confirmingStop = wasConfirming && state.status == SessionStatus.PAUSED,
            )
    }

    override fun pause() {
        session?.pause()
    }

    override fun resume() {
        session?.resume()
    }

    override fun next() {
        session?.next()
    }

    override fun previous() {
        session?.previous()
    }

    override fun requestStop() {
        val current = mutableState.value as? RunnerState.Active ?: return
        if (current.view.paused) mutableState.value = current.copy(confirmingStop = true)
    }

    override fun confirmStop() {
        val current = mutableState.value as? RunnerState.Active ?: return
        if (!current.confirmingStop) return
        mutableState.value = current.copy(confirmingStop = false)
        session?.stop()
    }

    override fun cancelStop() {
        val current = mutableState.value as? RunnerState.Active ?: return
        mutableState.value = current.copy(confirmingStop = false)
    }

    override fun back() {
        when (val current = mutableState.value) {
            is RunnerState.Starting, RunnerState.Unavailable -> {
                startJob?.cancel()
                mutableState.value = RunnerState.Exited
            }
            is RunnerState.Active ->
                when {
                    current.confirmingStop -> cancelStop()
                    current.view.paused -> requestStop()
                    else -> pause()
                }
            is RunnerState.Completed, RunnerState.Exited -> Unit
        }
    }

    /**
     * The screen left the foreground (EX-10): pauses a running workout, and keeps a workout still starting from
     * starting. The screen doesn't call it for a configuration change (NF-3, §4.3.4).
     */
    fun onScreenStopped() {
        inForeground.value = false
        session?.pause()
    }

    /** The screen is back in the foreground. A workout still starting may start now; a paused one stays paused. */
    fun onScreenStarted() {
        inForeground.value = true
    }

    companion object {
        /** The saved-state key of the flag that tells a rebuilt runner from a new one (§4.3.5). */
        internal const val STARTED_KEY = "started"

        /** Builds it for the workout [workoutId] with the app container's dependencies and the entry's saved state. */
        fun factory(workoutId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = (this[APPLICATION_KEY] as WorkoutTimerApp).container
                    RunnerViewModel(
                        workoutId = workoutId,
                        workoutRepository = container.workoutRepository,
                        settingsRepository = container.settingsRepository,
                        clock = container.clock,
                        cuePlayer = container.cuePlayer,
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            }

        /** The completed screen's summary: the planned total, and every round and exercise (P7 Q2). */
        private fun Timeline.summary(workoutName: String): WorkoutSummary {
            val first = (phases.first() as Phase.GetReady).first
            return WorkoutSummary(
                workoutName = workoutName,
                totalSeconds = totalSeconds,
                roundCount = first.roundCount,
                exerciseCount = phases.count { it is Phase.Exercise },
            )
        }
    }
}

/** What the runner shows, or the outcome it has finished with (P7 SPEC §4.2). */
sealed interface RunnerState {
    /** Reading the workout, then preparing the audio (§4.3.1). [workoutName] once the workout is read. */
    data class Starting(
        val workoutName: String? = null,
    ) : RunnerState

    /** No saved workout has this ID, or it isn't valid, so it can't run (§4.3.1). */
    data object Unavailable : RunnerState

    /** The workout is running or paused. [confirmingStop]: the Stop dialog is open (EX-8). */
    data class Active(
        val view: RunnerView,
        val confirmingStop: Boolean = false,
    ) : RunnerState

    /** The workout completed (EX-6). The screen calls `onCompleted(summary)` once. */
    data class Completed(
        val summary: WorkoutSummary,
    ) : RunnerState

    /**
     * The runner is done without completing: stopped (EX-8), left while starting or unavailable, or rebuilt after
     * Android closed the app (EX-11). The screen calls `onExit()` once.
     */
    data object Exited : RunnerState
}

/** What the runner screen can ask for. [RunnerViewModel] does it; `RunnerContent` only calls it. */
interface RunnerActions {
    /** Pauses a running workout (EX-4, EX-5). */
    fun pause()

    /** Resumes a paused workout with the time it had (EX-4). */
    fun resume()

    /** Moves to the next exercise while paused, or completes on the last one (EX-9). */
    fun next()

    /** Moves to the previous exercise while paused, or restarts the first one (EX-9). */
    fun previous()

    /** Opens the Stop dialog, while paused (EX-8). */
    fun requestStop()

    /** Stops the workout from the Stop dialog (EX-8). */
    fun confirmStop()

    /** Closes the Stop dialog; the workout stays paused. */
    fun cancelStop()

    /**
     * System Back, and "Back to list" when the workout can't run (P7 Q1): leaves while starting or unavailable,
     * pauses while running, opens the Stop dialog while paused, and closes it when it's open.
     */
    fun back()
}
