package io.github.davamix.workouttimer.core.settings

import kotlinx.coroutines.flow.Flow

/**
 * App-wide settings, changed on the settings screen (P6).
 *
 * @property voiceCuesEnabled Whether voice cues are spoken (TTS-9). On by default. With it off, the timer, the screen
 *   and the round-end sound work the same; only the voice is silent (P5).
 */
data class Settings(
    val voiceCuesEnabled: Boolean = true,
)

/**
 * Where the [Settings] are kept. P3 stores them in DataStore in the app; [InMemorySettingsRepository] is the version
 * for tests.
 */
interface SettingsRepository {
    /** The current settings on collection, then again after each change. It emits only when a value changes. */
    val settings: Flow<Settings>

    /** Turns voice cues on or off (TTS-9). The choice is remembered. */
    suspend fun setVoiceCuesEnabled(enabled: Boolean)
}
