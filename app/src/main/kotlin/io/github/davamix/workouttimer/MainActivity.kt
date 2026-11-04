package io.github.davamix.workouttimer

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.davamix.workouttimer.navigation.AppNavHost
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The volume keys always set the media volume, which the voice cues and the chime use, even between cues
        // (specification §5.3). Otherwise they change the ringer volume while nothing is playing.
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            WorkoutTimerTheme {
                AppNavHost()
            }
        }
    }
}
