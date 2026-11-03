package io.github.davamix.workouttimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.davamix.workouttimer.navigation.AppNavHost
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkoutTimerTheme {
                AppNavHost()
            }
        }
    }
}
