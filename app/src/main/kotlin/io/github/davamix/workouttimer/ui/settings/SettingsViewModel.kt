package io.github.davamix.workouttimer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The settings screen (P6 SPEC §4.3.7): the voice cues setting (TTS-9).
 *
 * @param prepareAudio Gets the audio ready, as `AppContainer.prepareAudio()` does (P5). Called after voice cues are
 *   turned on, so the first workout doesn't wait for the speech engine.
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val prepareAudio: suspend () -> Unit,
) : ViewModel() {
    private val saveFailed = MutableStateFlow(false)

    /** What the screen shows: the stored setting, once it has been read. */
    val state: StateFlow<SettingsState> =
        combine(
            repository.settings.map<Settings, Boolean?> { it.voiceCuesEnabled }.onStart { emit(null) },
            saveFailed,
        ) { voiceCuesEnabled, failed -> SettingsState(voiceCuesEnabled, failed) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    /**
     * Stores the voice setting. The screen shows the stored value, so if the write fails, the switch stays as it was
     * and [SettingsState.saveFailed] says so. Turning voice cues on also starts preparing the audio. Leaving the
     * screen may cancel that call, but not the preparation: the speech engine keeps connecting (P5).
     */
    fun setVoiceCuesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setVoiceCuesEnabled(enabled)
            } catch (_: IOException) {
                saveFailed.value = true
                return@launch
            }
            if (enabled) prepareAudio()
        }
    }

    /** The screen has shown the failure message. */
    fun messageShown() {
        saveFailed.value = false
    }

    companion object {
        /** Builds the ViewModel with the app's settings, and audio preparation off the main thread. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = (this[APPLICATION_KEY] as WorkoutTimerApp).container
                    SettingsViewModel(
                        repository = container.settingsRepository,
                        prepareAudio = { withContext(Dispatchers.Default) { container.prepareAudio() } },
                    )
                }
            }
    }
}

/**
 * What the settings screen shows.
 *
 * @property voiceCuesEnabled The stored voice setting, or `null` until it has been read.
 * @property saveFailed Whether the last change couldn't be stored: shown once, in a snackbar.
 */
data class SettingsState(
    val voiceCuesEnabled: Boolean? = null,
    val saveFailed: Boolean = false,
)
