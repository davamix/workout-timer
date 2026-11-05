package io.github.davamix.workouttimer.ui.completed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.formatTotalDuration

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

/**
 * The completed screen (EX-6, P7 SPEC §4.3.8). It has no ViewModel: its route carries the [summary], so it also comes
 * back as it was if Android closes the app while it's open. System Back returns to the list, as [onBackToList] does:
 * the runner is no longer on the back stack.
 */
@Composable
fun CompletedScreen(
    summary: WorkoutSummary,
    onBackToList: () -> Unit,
) {
    CompletedContent(summary, onBackToList)
}

/** "Well done!", the workout's name, its total time and counts, and Back to list. */
@Composable
fun CompletedContent(
    summary: WorkoutSummary,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rounds = pluralStringResource(R.plurals.completed_rounds, summary.roundCount, summary.roundCount)
    val exercises = pluralStringResource(R.plurals.completed_exercises, summary.exerciseCount, summary.exerciseCount)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.completed_title),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("completed-title"),
            )
            Text(
                text = stringResource(R.string.completed_workout, summary.workoutName),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("completed-workout"),
            )
            Text(
                text = stringResource(R.string.completed_summary, formatTotalDuration(summary.totalSeconds), rounds, exercises),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("completed-summary"),
            )
            Button(
                onClick = onBackToList,
                modifier = Modifier.padding(top = 16.dp).heightIn(min = 72.dp).widthIn(min = 240.dp),
            ) {
                Text(stringResource(R.string.completed_back_to_list), fontSize = 28.sp)
            }
        }
    }
}
