package io.github.davamix.workouttimer.audiodebug

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme

/**
 * The audio debug screen's activity (P5 SPEC §4.3.7), declared only in the debug manifest, with its own launcher
 * icon, "WT audio debug". Its ViewModel owns the running session, so turning the tablet keeps it.
 */
class AudioDebugActivity : ComponentActivity() {
    private val viewModel: AudioDebugViewModel by viewModels {
        viewModelFactory { initializer { AudioDebugViewModel((application as WorkoutTimerApp).container) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // As in MainActivity: the volume keys always set the media volume (specification §5.3).
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            WorkoutTimerTheme {
                AudioDebugScreen(viewModel)
            }
        }
    }
}
