package io.github.davamix.workouttimer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Background colour of each runner screen state, so it can be recognised from a distance (spec UI-7), and the colour
 * of the text on it. Each text colour has a contrast of at least 4.5:1 with its backgrounds (P7 SPEC §4.3.9).
 */
object PhaseColors {
    val GetReady = Color(0xFF6A1B9A)
    val Exercise = Color(0xFF2E7D32)
    val Rest = Color(0xFF1565C0)
    val Paused = Color(0xFFEF6C00)

    /** Text on [GetReady] (9.4:1), [Exercise] (5.1:1) and [Rest] (5.8:1). */
    val OnPhase = Color.White

    /** Text on [Paused] (6.8:1). White would be only 3.1:1. */
    val OnPaused = Color.Black
}

internal val Green200 = Color(0xFF81C784)
internal val Green900 = Color(0xFF003910)
internal val Background = Color(0xFF121212)
