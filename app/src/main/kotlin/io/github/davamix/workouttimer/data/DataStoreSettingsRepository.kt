package io.github.davamix.workouttimer.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

/** The voice-cue setting (TTS-9). A missing key means the default. */
private val VOICE_CUES_ENABLED = booleanPreferencesKey("voice_cues_enabled")

/**
 * A [SettingsRepository] backed by a Preferences DataStore (plan D-5). Create the DataStore with
 * [createSettingsDataStore].
 *
 * Reading never fails: a missing value gives its default, and an I/O error gives the defaults. Writing propagates
 * I/O errors to the caller.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<Settings> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { it.toSettings() }
            .distinctUntilChanged()

    override suspend fun setVoiceCuesEnabled(enabled: Boolean) {
        dataStore.edit { it[VOICE_CUES_ENABLED] = enabled }
    }
}

private fun Preferences.toSettings() =
    Settings(
        voiceCuesEnabled = this[VOICE_CUES_ENABLED] ?: Settings().voiceCuesEnabled,
    )

/**
 * The settings DataStore. A corrupted file is replaced with empty preferences, so the settings fall back to their
 * defaults, and [onWarning] is called.
 *
 * Only one DataStore may be active per file in a process, or DataStore throws: the app's `AppContainer` creates it
 * once. A DataStore stops being active when its [scope] is cancelled.
 *
 * @param file Where the settings are stored. Its name must end in `.preferences_pb`.
 */
fun createSettingsDataStore(
    file: File,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    onWarning: (message: String, error: Throwable) -> Unit = { _, _ -> },
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler =
            ReplaceFileCorruptionHandler { error ->
                onWarning("The settings file ${file.name} was damaged; settings are back to their defaults", error)
                emptyPreferences()
            },
        scope = scope,
        produceFile = { file },
    )
