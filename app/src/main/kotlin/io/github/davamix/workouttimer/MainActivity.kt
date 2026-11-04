package io.github.davamix.workouttimer

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.github.davamix.workouttimer.navigation.AppNavHost
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The volume keys always set the media volume, which the voice cues and the chime use, even between cues
        // (specification §5.3). Otherwise they change the ringer volume while nothing is playing.
        volumeControlStream = AudioManager.STREAM_MUSIC
        // Connecting and warming up the TTS engine takes 9–10 s from a cold start, so it starts now, off the main
        // thread, rather than when a workout starts. If the activity goes first, the warm-up still carries on.
        lifecycleScope.launch(Dispatchers.Default) { (application as WorkoutTimerApp).container.prepareAudio() }
        setContent {
            WorkoutTimerTheme {
                AppNavHost()
            }
        }
    }
}
