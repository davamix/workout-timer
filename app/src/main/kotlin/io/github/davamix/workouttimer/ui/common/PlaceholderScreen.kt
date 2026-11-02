package io.github.davamix.workouttimer.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.davamix.workouttimer.R

/** Temporary screen body used until the real screens arrive in P6 and P7. Delete it with the last placeholder. */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = stringResource(R.string.placeholder_note),
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
            actions()
        }
    }
}
