package io.github.davamix.workouttimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark only: high contrast for a screen watched from a distance (P1-D3).
private val DarkColors =
    darkColorScheme(
        primary = Green200,
        onPrimary = Green900,
        background = Background,
        surface = Background,
    )

@Composable
fun WorkoutTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
