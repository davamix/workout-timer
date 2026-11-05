package io.github.davamix.workouttimer.ui.runner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The runner buttons' icons, drawn here from simple shapes on a 24 × 24 grid (P7 SPEC §4.3.9). `material-icons-core`
 * has only the play arrow of these, and the extended set is very large. They're our own drawing, so no third-party
 * artwork comes into the repo. `Icon` tints them with the content colour.
 */
internal object RunnerIcons {
    /** Two bars. */
    val Pause: ImageVector =
        icon("Pause") {
            rect(6f, 5f, 4f, 14f)
            rect(14f, 5f, 4f, 14f)
        }

    /** A triangle pointing right. */
    val Resume: ImageVector =
        icon("Resume") {
            triangle(7f, 5f, 19f, 12f, 7f, 19f)
        }

    /** A bar, then a triangle pointing left. */
    val Previous: ImageVector =
        icon("Previous") {
            rect(6f, 6f, 2.5f, 12f)
            triangle(18f, 6f, 9.5f, 12f, 18f, 18f)
        }

    /** A triangle pointing right, then a bar. */
    val Next: ImageVector =
        icon("Next") {
            triangle(6f, 6f, 14.5f, 12f, 6f, 18f)
            rect(15.5f, 6f, 2.5f, 12f)
        }

    /** A square. */
    val Stop: ImageVector =
        icon("Stop") {
            rect(6f, 6f, 12f, 12f)
        }
}

private fun icon(
    name: String,
    shapes: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = SolidColor(Color.Black), pathBuilder = shapes)
        .build()

private fun PathBuilder.rect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
) {
    moveTo(x, y)
    horizontalLineToRelative(width)
    verticalLineToRelative(height)
    horizontalLineToRelative(-width)
    close()
}

private fun PathBuilder.triangle(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float,
) {
    moveTo(x1, y1)
    lineTo(x2, y2)
    lineTo(x3, y3)
    close()
}
