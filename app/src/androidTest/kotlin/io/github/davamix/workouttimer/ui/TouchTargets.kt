package io.github.davamix.workouttimer.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

/**
 * Asserts that every clickable node on screen, dialogs included, has a touch area of at least [min] × [min] (NF-6,
 * P6 SPEC §4.3.8). The touch area includes the padding Material adds to small components.
 */
fun ComposeTestRule.assertTouchTargetsAtLeast(min: Dp = 48.dp) {
    val nodes = onAllNodes(hasClickAction()).fetchSemanticsNodes()
    assertTrue("No clickable nodes found", nodes.isNotEmpty())
    // Allow for rounding to whole pixels.
    val minPx = with(density) { min.toPx() } - 0.5f
    val small = nodes.filter { it.touchBoundsInRoot.width < minPx || it.touchBoundsInRoot.height < minPx }
    assertTrue("Touch targets smaller than $min: ${small.joinToString { it.describe() }}", small.isEmpty())
}

private fun SemanticsNode.describe(): String {
    val label =
        config.getOrNull(SemanticsProperties.Text)?.joinToString()
            ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: config.getOrNull(SemanticsProperties.TestTag)
            ?: "node $id"
    return "$label (${touchBoundsInRoot.width} × ${touchBoundsInRoot.height} px)"
}
