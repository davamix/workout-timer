package io.github.davamix.workouttimer.ui.completed

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.PlaceholderScreen

// Placeholder until P7.
@Composable
fun CompletedScreen(onBackToList: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.completed_title)) {
        Button(onClick = onBackToList) { Text(stringResource(R.string.completed_back_to_list)) }
    }
}
