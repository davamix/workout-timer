package io.github.davamix.workouttimer.ui.editor

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.PlaceholderScreen

// Placeholder until P6.
@Composable
fun WorkoutEditorScreen(
    workoutId: String?,
    onBack: () -> Unit,
) {
    val title = if (workoutId == null) R.string.editor_title_new else R.string.editor_title_edit
    PlaceholderScreen(title = stringResource(title)) {
        Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}
