package io.github.davamix.workouttimer.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.davamix.workouttimer.core.engine.Clock
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.settings.SettingsRepository
import io.github.davamix.workouttimer.data.DataStoreSettingsRepository
import io.github.davamix.workouttimer.data.FileWorkoutRepository
import io.github.davamix.workouttimer.data.createSettingsDataStore
import java.io.File

/**
 * Creates and holds the app's long-lived objects (plan D-3). Each phase adds the dependencies it provides here:
 * storage in P3 and audio in P5.
 */
class AppContainer(
    val context: Context,
) {
    /** Saved workouts, one JSON file each in `<filesDir>/workouts` (P3). Created on first use. */
    val workoutRepository: WorkoutRepository by lazy {
        FileWorkoutRepository(File(context.filesDir, "workouts"), onWarning = ::logStorageWarning)
    }

    /**
     * App settings, in `<filesDir>/datastore/settings.preferences_pb` (P3). Created on first use; DataStore allows
     * only one instance per file, so this is the only one.
     */
    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(
            createSettingsDataStore(context.preferencesDataStoreFile("settings"), onWarning = ::logStorageWarning),
        )
    }

    /**
     * The monotonic clock the workout engine runs on (P4, plan D-6). `elapsedRealtime` keeps counting in deep sleep
     * and ignores changes to the device's date or time.
     */
    val clock: Clock = Clock { SystemClock.elapsedRealtime() }

    /** Storage problems that don't stop the app, such as a damaged file. Logged in debug builds only (plan §3). */
    private fun logStorageWarning(
        message: String,
        error: Throwable,
    ) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.w(STORAGE_LOG_TAG, message, error)
        }
    }

    private companion object {
        const val STORAGE_LOG_TAG = "WorkoutStorage"
    }
}
