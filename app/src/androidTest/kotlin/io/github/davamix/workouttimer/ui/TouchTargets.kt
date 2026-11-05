package io.github.davamix.workouttimer.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import kotlin.math.abs

/**
 * Asserts that every clickable node fully on screen, dialogs included, has a touch area of at least [min] × [min]
 * (NF-6, P6 SPEC §4.3.8).
 *
 * The touch area includes what Material's `minimumInteractiveComponentSize` adds: an icon button lays out at 40 dp but
 * takes touches over 48 dp. Touch bounds are clipped to the screen, so nodes partly scrolled out of view are skipped.
 */
fun ComposeTestRule.assertTouchTargetsAtLeast(min: Dp = 48.dp) {
    val visible = onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { it.isFullyVisible() }
    assertTrue("No clickable nodes on screen", visible.isNotEmpty())
    // Allow for rounding to whole pixels.
    val minPx = with(density) { min.toPx() } - 1f
    val small = visible.filter { it.touchBoundsInRoot.width < minPx || it.touchBoundsInRoot.height < minPx }
    assertTrue("Touch targets smaller than $min: ${small.joinToString { it.describe() }}", small.isEmpty())
}

/** Whether nothing of this node is clipped: its visible bounds are as large as its layout. */
private fun SemanticsNode.isFullyVisible(): Boolean =
    abs(boundsInRoot.width - size.width) < 1f && abs(boundsInRoot.height - size.height) < 1f

private fun SemanticsNode.describe(): String {
    val label =
        config.getOrNull(SemanticsProperties.Text)?.joinToString()
            ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: config.getOrNull(SemanticsProperties.TestTag)
            ?: "node $id"
    return "$label (${touchBoundsInRoot.width} × ${touchBoundsInRoot.height} px)"
}
