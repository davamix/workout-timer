package io.github.davamix.workouttimer.ui.runner

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.PlaceholderScreen

// Placeholder until P7.
@Composable
fun RunnerScreen(
    workoutId: String,
    onFinished: () -> Unit,
) {
    PlaceholderScreen(
        title = stringResource(R.string.runner_title),
        subtitle = stringResource(R.string.runner_workout_id, workoutId),
    ) {
        Button(onClick = onFinished) { Text(stringResource(R.string.runner_finish)) }
    }
}
