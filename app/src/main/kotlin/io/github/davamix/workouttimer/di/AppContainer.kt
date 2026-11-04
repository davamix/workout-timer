package io.github.davamix.workouttimer.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.davamix.workouttimer.audio.CuePlayer
import io.github.davamix.workouttimer.audio.MediaSoundPlayer
import io.github.davamix.workouttimer.audio.SoundPlayer
import io.github.davamix.workouttimer.audio.Speaker
import io.github.davamix.workouttimer.audio.SpokenText
import io.github.davamix.workouttimer.audio.TtsSpeaker
import io.github.davamix.workouttimer.core.engine.Clock
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.settings.SettingsRepository
import io.github.davamix.workouttimer.data.DataStoreSettingsRepository
import io.github.davamix.workouttimer.data.FileWorkoutRepository
import io.github.davamix.workouttimer.data.createSettingsDataStore
import kotlinx.coroutines.flow.first
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

    /** The English text of the voice cues (P5). */
    private val spokenText by lazy { SpokenText(context.resources) }

    /**
     * Speaks the voice cues with the English (US) voice (P5). Created on first use, and connected and warmed up with
     * the fixed phrases by [CuePlayer.prepare]; it lives as long as the app.
     */
    val speaker: Speaker by lazy { TtsSpeaker(context, spokenText.fixedPhrases, onWarning = ::logAudioWarning) }

    /** Plays the round-end chime (P5). Created on first use; it lives as long as the app. */
    val soundPlayer: SoundPlayer by lazy { MediaSoundPlayer(context, onWarning = ::logAudioWarning) }

    /** Plays a running workout's voice and sound cues (P5). One workout at a time. */
    val cuePlayer: CuePlayer by lazy { CuePlayer(speaker, soundPlayer, spokenText::of) }

    /**
     * Gets the audio ready before any workout starts (P5): loads the chime and, if voice cues are on, prepares the
     * speaker. The activities call it when they're created, so a workout rarely waits for the TTS engine, which
     * takes 9–10 s from a cold start on the tablet.
     */
    suspend fun prepareAudio() {
        cuePlayer.prepare(settingsRepository.settings.first().voiceCuesEnabled)
    }

    /** Storage problems that don't stop the app, such as a damaged file. Logged in debug builds only (plan §3). */
    private fun logStorageWarning(
        message: String,
        error: Throwable,
    ) {
        if (isDebuggable) Log.w(STORAGE_LOG_TAG, message, error)
    }

    /** Audio problems, such as a missing voice. They never stop the workout. Logged in debug builds only (plan §3). */
    private fun logAudioWarning(
        message: String,
        error: Throwable?,
    ) {
        if (isDebuggable) Log.w(AUDIO_LOG_TAG, message, error)
    }

    private val isDebuggable: Boolean
        get() = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private companion object {
        const val STORAGE_LOG_TAG = "WorkoutStorage"
        const val AUDIO_LOG_TAG = "WorkoutAudio"
    }
}
