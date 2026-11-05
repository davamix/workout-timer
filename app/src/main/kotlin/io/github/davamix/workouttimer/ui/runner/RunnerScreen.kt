package io.github.davamix.workouttimer.ui.runner

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.completed.WorkoutSummary
import io.github.davamix.workouttimer.ui.theme.PhaseColors
import kotlinx.coroutines.flow.first

/**
 * The runner screen (P7 SPEC §4.3): it runs the workout [workoutId] through its [viewModel], and draws it with
 * [RunnerContent].
 *
 * - It keeps the screen on while it's shown (NF-2, §4.3.6).
 * - It pauses the workout when the app leaves the foreground, but not for a configuration change (EX-10, NF-3,
 *   §4.3.4).
 * - System Back goes to the ViewModel (P7 Q1).
 * - It reports each outcome once, after waiting until it's resumed (§4.3.7). A runner rebuilt after Android closed the
 *   app is composed before it's resumed, so the callbacks mustn't go through `dropUnlessResumed`, which would drop them.
 *
 * @param onCompleted The workout completed: open the completed screen with this summary (EX-6).
 * @param onExit The runner is done without completing: return to the workout list (EX-8, EX-11, Back).
 */
@Composable
fun RunnerScreen(
    workoutId: String,
    onCompleted: (WorkoutSummary) -> Unit,
    onExit: () -> Unit,
    viewModel: RunnerViewModel = viewModel(factory = RunnerViewModel.factory(workoutId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // A configuration change stops the activity too, just before recreating it; the workout carries on.
        if (activity?.isChangingConfigurations != true) viewModel.onScreenStopped()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onScreenStarted() }
    BackHandler(enabled = state !is RunnerState.Completed && state !is RunnerState.Exited) { viewModel.back() }

    val outcome = state
    LaunchedEffect(outcome) {
        if (outcome !is RunnerState.Completed && outcome !is RunnerState.Exited) return@LaunchedEffect
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        if (outcome is RunnerState.Completed) onCompleted(outcome.summary) else onExit()
    }

    RunnerContent(state, viewModel)
}

/**
 * Draws the runner's [state] (P7 SPEC §4.3.1–§4.3.3, §4.3.9). Every action goes to [actions]. It draws nothing for
 * the outcomes, [RunnerState.Completed] and [RunnerState.Exited]: the screen leaves at once.
 */
@Composable
fun RunnerContent(
    state: RunnerState,
    actions: RunnerActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is RunnerState.Starting -> Starting(state.workoutName, modifier)
        RunnerState.Unavailable -> Unavailable(onBack = actions::back, modifier = modifier)
        is RunnerState.Active -> {
            Active(state.view, actions, modifier)
            if (state.confirmingStop) StopDialog(actions)
        }
        is RunnerState.Completed, RunnerState.Exited ->
            Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
}

/** Get ready's background, the workout's name once read, and a progress indicator (P7 Q3). */
@Composable
private fun Starting(
    workoutName: String?,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides PhaseColors.OnPhase) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(PhaseColors.GetReady)
                    .padding(32.dp)
                    .testTag("runner-starting"),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = workoutName.orEmpty(),
                fontSize = HeaderSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CircularProgressIndicator(color = PhaseColors.OnPhase, modifier = Modifier.size(72.dp))
            Text(stringResource(R.string.runner_starting), fontSize = HeaderSize)
        }
    }
}

/** The workout is missing or invalid: a message, and the way back (§4.3.1). */
@Composable
private fun Unavailable(
    onBack: () -> Unit,
    modifier: Modifier,
) {
    // The surface gives the text the theme's colour on its background.
    Surface(modifier = modifier.fillMaxSize().testTag("runner-unavailable"), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.runner_unavailable), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onBack, modifier = Modifier.heightIn(min = DialogButtonHeight)) {
                Text(stringResource(R.string.runner_back_to_list), fontSize = DialogButtonTextSize)
            }
        }
    }
}

/** A running or paused workout: header, title, time, next lines and buttons, on the state's colour (§4.3.2). */
@Composable
private fun Active(
    view: RunnerView,
    actions: RunnerActions,
    modifier: Modifier,
) {
    val background =
        when {
            view.paused -> PhaseColors.Paused
            view.kind == PhaseKind.GET_READY -> PhaseColors.GetReady
            view.kind == PhaseKind.EXERCISE -> PhaseColors.Exercise
            else -> PhaseColors.Rest
        }
    val content = if (view.paused) PhaseColors.OnPaused else PhaseColors.OnPhase
    CompositionLocalProvider(LocalContentColor provides content) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(background)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    .testTag("runner-active"),
            verticalArrangement = Arrangement.spacedBy(Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(view)
            Title(view)
            if (view.paused) {
                Text(
                    text = stringResource(R.string.runner_paused),
                    fontSize = PausedSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("runner-paused"),
                )
            }
            Time(view.remainingSeconds, Modifier.weight(1f).fillMaxWidth())
            NextLines(view)
            if (view.paused) PausedButtons(view, actions, background) else RunningButtons(actions, background)
        }
    }
}

/** "Round 2/3 · Legs" on the left; "Exercise 2/4" on the right during an exercise (UI-1, UI-6). */
@Composable
private fun Header(view: RunnerView) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.runner_round, view.round.number, view.round.count, view.round.name),
            fontSize = HeaderSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("runner-round"),
        )
        view.exercise?.let {
            Spacer(Modifier.width(24.dp))
            Text(
                text = stringResource(R.string.runner_exercise, it.number, it.count),
                fontSize = HeaderSize,
                maxLines = 1,
                modifier = Modifier.testTag("runner-exercise-number"),
            )
        }
    }
}

/** The exercise's name as written, or "GET READY" or "REST" (UI-2, EX-7, UI-5; P7 Q6). Long names shrink. */
@Composable
private fun Title(view: RunnerView) {
    val title =
        when (view.kind) {
            PhaseKind.GET_READY -> stringResource(R.string.runner_get_ready)
            PhaseKind.REST -> stringResource(R.string.runner_rest)
            PhaseKind.EXERCISE -> view.exercise?.name.orEmpty()
        }
    Box(modifier = Modifier.fillMaxWidth().height(TitleHeight), contentAlignment = Alignment.Center) {
        BasicText(
            text = title,
            style = TextStyle(color = LocalContentColor.current, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = TitleMinSize, maxFontSize = TitleMaxSize),
            modifier = Modifier.testTag("runner-title"),
        )
    }
}

/** The phase's remaining time, `mm:ss` (UI-3), as large as the space left allows. */
@Composable
private fun Time(
    remainingSeconds: Int,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = formatPhaseTime(remainingSeconds),
            // Digits of equal width, so the time doesn't shift as it counts.
            style =
                TextStyle(
                    color = LocalContentColor.current,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = TimeMinSize, maxFontSize = TimeMaxSize),
            modifier = Modifier.testTag("runner-time"),
        )
    }
}

/** During a round rest, the next round; during get ready and rests, the next exercise (EX-7, UI-5). */
@Composable
private fun NextLines(view: RunnerView) {
    view.nextRound?.let {
        Text(
            text = stringResource(R.string.runner_next_round, it.name),
            fontSize = NextSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("runner-next-round"),
        )
    }
    view.next?.let {
        Text(
            text = stringResource(R.string.runner_next_exercise, it.name, it.durationSeconds),
            fontSize = NextSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("runner-next-exercise"),
        )
    }
}

/** Running: only Pause (UI-10). */
@Composable
private fun RunningButtons(
    actions: RunnerActions,
    background: Color,
) {
    MainButton(R.string.runner_pause, RunnerIcons.Pause, actions::pause, background, "runner-button-pause")
}

/**
 * Paused: one row with Previous, Resume and Next, then a gap and Stop (UI-11, §4.3.3). Stop in the same row, rather
 * than under it, leaves the time room to stay large (P7 Q7).
 */
@Composable
private fun PausedButtons(
    view: RunnerView,
    actions: RunnerActions,
    background: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap), verticalAlignment = Alignment.CenterVertically) {
        SideButton(
            R.string.runner_previous,
            RunnerIcons.Previous,
            actions::previous,
            "runner-button-previous",
            enabled = view.canGoPrevious,
        )
        MainButton(R.string.runner_resume, RunnerIcons.Resume, actions::resume, background, "runner-button-resume")
        SideButton(R.string.runner_next, RunnerIcons.Next, actions::next, "runner-button-next")
        Spacer(Modifier.width(StopGap))
        SideButton(R.string.runner_stop, RunnerIcons.Stop, actions::requestStop, "runner-button-stop")
    }
}

/** Pause or Resume: filled in the text colour, with the background colour as its content (§4.3.9). */
@Composable
private fun MainButton(
    @StringRes label: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    background: Color,
    tag: String,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = LocalContentColor.current, contentColor = background),
        modifier = Modifier.heightIn(min = MainButtonHeight).widthIn(min = MainButtonWidth).testTag(tag),
    ) {
        ButtonLabel(label, icon)
    }
}

/** Previous, Next and Stop: outlined in the text colour (§4.3.9). Disabled, it's dimmed. */
@Composable
private fun SideButton(
    @StringRes label: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true,
) {
    val content = LocalContentColor.current
    val border = if (enabled) content else content.copy(alpha = DISABLED_ALPHA)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = content,
                disabledContentColor = content.copy(alpha = DISABLED_ALPHA),
            ),
        border = BorderStroke(2.dp, border),
        modifier = Modifier.heightIn(min = SideButtonHeight).widthIn(min = SideButtonWidth).testTag(tag),
    ) {
        ButtonLabel(label, icon)
    }
}

/** The icon is decorative: the text names the action. */
@Composable
private fun ButtonLabel(
    @StringRes label: Int,
    icon: ImageVector,
) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
    Spacer(Modifier.width(12.dp))
    Text(stringResource(label), fontSize = ButtonTextSize)
}

/** "Stop the workout?" (EX-8). Cancel, Back and a tap outside close it. */
@Composable
private fun StopDialog(actions: RunnerActions) {
    AlertDialog(
        onDismissRequest = actions::cancelStop,
        title = { Text(stringResource(R.string.runner_stop_title)) },
        text = { Text(stringResource(R.string.runner_stop_text)) },
        confirmButton = {
            Button(
                onClick = actions::confirmStop,
                modifier = Modifier.heightIn(min = DialogButtonHeight).testTag("runner-stop-confirm"),
            ) {
                Text(stringResource(R.string.runner_stop_confirm), fontSize = DialogButtonTextSize)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = actions::cancelStop,
                modifier = Modifier.heightIn(min = DialogButtonHeight).testTag("runner-stop-cancel"),
            ) {
                Text(stringResource(R.string.runner_stop_cancel), fontSize = DialogButtonTextSize)
            }
        },
    )
}

// Sizes (P7 SPEC §4.3.9). The content area is 960 × 542 dp on the tablet (T1).
private val Gap = 8.dp
private val HeaderSize = 30.sp
private val TitleHeight = 80.dp
private val TitleMinSize = 36.sp
private val TitleMaxSize = 72.sp
private val PausedSize = 36.sp
private val TimeMinSize = 48.sp
private val TimeMaxSize = 220.sp
private val NextSize = 30.sp
private val MainButtonHeight = 96.dp
private val MainButtonWidth = 240.dp
private val SideButtonHeight = 80.dp
private val SideButtonWidth = 170.dp
private val ButtonGap = 16.dp
private val StopGap = 24.dp
private val ButtonIconSize = 36.dp
private val ButtonTextSize = 28.sp
private val DialogButtonHeight = 56.dp
private val DialogButtonTextSize = 22.sp
private const val DISABLED_ALPHA = 0.38f
