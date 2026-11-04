package io.github.davamix.workouttimer.audiodebug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.engine.SessionState
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.Timeline
import java.util.Locale

/**
 * The audio debug screen (P5 SPEC §4.3.7): choose a workout, switch voice cues, run it, and watch the remaining
 * seconds and the cues while listening.
 */
@Composable
fun AudioDebugScreen(viewModel: AudioDebugViewModel) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val voiceOn by viewModel.voiceCuesEnabled.collectAsStateWithLifecycle()
    val speakerStatus by viewModel.speaker.status.collectAsStateWithLifecycle()
    val preparing by viewModel.preparing.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val state = session?.state?.collectAsStateWithLifecycle()?.value
    val log by viewModel.log.collectAsStateWithLifecycle()

    val status = state?.status
    val active = status == SessionStatus.NOT_STARTED || status == SessionStatus.RUNNING || status == SessionStatus.PAUSED

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(modifier = Modifier.width(360.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.audio_debug_workout), style = MaterialTheme.typography.titleLarge)
                viewModel.workouts.forEachIndexed { index, workout ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth().selectable(
                                selected = index == selected,
                                enabled = !active,
                                onClick = { viewModel.select(index) },
                            ),
                    ) {
                        RadioButton(selected = index == selected, onClick = null, enabled = !active)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                R.string.audio_debug_workout_option,
                                workout.name,
                                minutesAndSeconds(Timeline.of(workout).totalSeconds),
                            ),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.audio_debug_voice_cues), modifier = Modifier.weight(1f))
                    Switch(checked = voiceOn, onCheckedChange = viewModel::setVoiceCuesEnabled, enabled = !active)
                }
                Text(stringResource(R.string.audio_debug_speaker, speakerStatus.name))
                Text(
                    stringResource(
                        R.string.audio_debug_voice,
                        viewModel.speaker.voiceName ?: stringResource(R.string.audio_debug_no_voice),
                    ),
                )
                Spacer(Modifier.weight(1f))
                ButtonRow {
                    DebugButton(R.string.audio_debug_start, enabled = !active && !preparing, onClick = viewModel::start)
                    DebugButton(R.string.audio_debug_stop, enabled = active, onClick = { viewModel.stop() })
                }
                ButtonRow {
                    DebugButton(R.string.audio_debug_pause, enabled = status == SessionStatus.RUNNING, onClick = { viewModel.pause() })
                    DebugButton(R.string.audio_debug_resume, enabled = status == SessionStatus.PAUSED, onClick = { viewModel.resume() })
                }
                ButtonRow {
                    DebugButton(R.string.audio_debug_previous, enabled = state?.canGoPrevious == true, onClick = { viewModel.previous() })
                    DebugButton(R.string.audio_debug_next, enabled = status == SessionStatus.PAUSED, onClick = { viewModel.next() })
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    when {
                        preparing -> stringResource(R.string.audio_debug_preparing)
                        state == null -> stringResource(R.string.audio_debug_not_started)
                        else -> describe(state)
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    state?.remainingSeconds?.toString() ?: "–",
                    fontSize = 160.sp,
                    lineHeight = 170.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(stringResource(R.string.audio_debug_log), style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(log) { line -> Text(line, fontFamily = FontFamily.Monospace, fontSize = 14.sp) }
                }
            }
        }
    }
}

@Composable
private fun ButtonRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun DebugButton(
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.width(170.dp)) { Text(stringResource(label)) }
}

@Composable
private fun describe(state: SessionState): String {
    val phase =
        when (val phase = state.phase) {
            is Phase.GetReady -> "Get ready → ${phase.first.name}"
            is Phase.Exercise -> "${phase.exercise.roundName} · ${phase.exercise.name}"
            is Phase.Rest -> "Rest (${phase.kind.name.lowercase(Locale.ROOT)}) → ${phase.next.name}"
        }
    val waiting = if (state.awaitingAnnouncement) ", " + stringResource(R.string.audio_debug_waiting) else ""
    return "${state.status.name} · $phase$waiting"
}

private fun minutesAndSeconds(seconds: Int): String = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
