package io.github.davamix.workouttimer.ui.list

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.PlaceholderScreen

// Placeholder until P6.
@Composable
fun WorkoutListScreen(
    onNewWorkout: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (workoutId: String) -> Unit,
) {
    PlaceholderScreen(title = stringResource(R.string.list_title)) {
        Button(onClick = onNewWorkout) { Text(stringResource(R.string.list_new_workout)) }
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.list_settings)) }
        Button(onClick = { onStartWorkout(SAMPLE_WORKOUT_ID) }) { Text(stringResource(R.string.list_run_sample)) }
    }
}

private const val SAMPLE_WORKOUT_ID = "sample"
