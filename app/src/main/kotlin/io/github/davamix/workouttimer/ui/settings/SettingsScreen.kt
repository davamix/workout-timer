package io.github.davamix.workouttimer.ui.settings

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.PlaceholderScreen

// Placeholder until P6.
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.settings_title)) {
        Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}
