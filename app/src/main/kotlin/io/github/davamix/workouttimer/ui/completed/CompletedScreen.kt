package io.github.davamix.workouttimer.ui.completed

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.PlaceholderScreen

/**
 * What the completed screen shows (specification §6). All three numbers are the workout's planned values, so pauses
 * and skipped exercises don't change them (P4 Q2, P7 Q2).
 *
 * @property workoutName The workout's name.
 * @property totalSeconds The planned total from Start to completion, the timeline's (WM-6).
 * @property roundCount How many rounds the workout has.
 * @property exerciseCount How many exercises the workout has, in all its rounds.
 */
data class WorkoutSummary(
    val workoutName: String,
    val totalSeconds: Int,
    val roundCount: Int,
    val exerciseCount: Int,
)

// Placeholder until P7 T5.
@Composable
fun CompletedScreen(onBackToList: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.completed_title)) {
        Button(onClick = onBackToList) { Text(stringResource(R.string.completed_back_to_list)) }
    }
}
