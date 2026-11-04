package io.github.davamix.workouttimer.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.model.TimeField
import io.github.davamix.workouttimer.ui.common.formatTotalDuration

/**
 * The workout editor (P6 SPEC §4.3.2–§4.3.5). [onClose] is called once: after saving, or after leaving without saving.
 *
 * @param workoutId The saved workout to edit, or `null` for a new one.
 */
@Composable
fun WorkoutEditorScreen(
    workoutId: String?,
    onClose: () -> Unit,
    viewModel: WorkoutEditorViewModel = viewModel(factory = WorkoutEditorViewModel.factory(workoutId)),
) {
    val state = viewModel.state
    if (state is EditorState.Closed) {
        LaunchedEffect(Unit) { onClose() }
    }
    BackHandler(enabled = state !is EditorState.Closed) { viewModel.requestClose() }
    WorkoutEditorContent(state, viewModel)
}

/** Draws [state]. Every action goes to [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorContent(
    state: EditorState,
    actions: EditorActions,
) {
    val editing = state as? EditorState.Editing
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var scrollToRoundId by remember { mutableStateOf<String?>(null) }
    var focusExerciseId by remember { mutableStateOf<String?>(null) }

    val message = editing?.message
    val messageText = message?.let { stringResource(it.text) }
    LaunchedEffect(message) {
        if (message == null || messageText == null) return@LaunchedEffect
        if (message == EditorMessage.CHECK_FIELDS) {
            editing?.let { firstItemWithError(it.form, it.errors) }?.let { listState.animateScrollToItem(it) }
        }
        snackbarHostState.showSnackbar(messageText)
        actions.messageShown()
    }
    LaunchedEffect(scrollToRoundId) {
        val id = scrollToRoundId ?: return@LaunchedEffect
        val index = editing?.form?.rounds?.indexOfFirst { it.id == id } ?: -1
        if (index >= 0) listState.animateScrollToItem(FIRST_ROUND_ITEM + index)
        scrollToRoundId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = actions::requestClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = { Text(stringResource(if (editing?.isNew == true) R.string.editor_title_new else R.string.editor_title_edit)) },
                actions = {
                    if (editing != null) {
                        Text(
                            text = stringResource(R.string.editor_total, formatTotalDuration(editing.totalSeconds)),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("editor-total"),
                        )
                        Spacer(Modifier.width(24.dp))
                        OutlinedButton(onClick = actions::requestClose, modifier = Modifier.heightIn(min = ActionHeight)) {
                            Text(stringResource(R.string.editor_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = actions::save,
                            enabled = !editing.saving,
                            modifier = Modifier.heightIn(min = ActionHeight),
                        ) {
                            Text(stringResource(R.string.editor_save))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                EditorState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                EditorState.NotFound -> NotFound(onBack = actions::requestClose, modifier = Modifier.align(Alignment.Center))
                is EditorState.Editing ->
                    EditorForm(
                        state = state,
                        actions = actions,
                        listState = listState,
                        focusExerciseId = focusExerciseId,
                        onFocusHandled = { focusExerciseId = null },
                        onExerciseAdded = { focusExerciseId = it },
                        onRoundAdded = { scrollToRoundId = it },
                    )
                EditorState.Closed -> Unit
            }
        }
    }

    if (editing?.confirmingDiscard == true) {
        AlertDialog(
            onDismissRequest = actions::cancelDiscard,
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(stringResource(R.string.editor_discard_text)) },
            confirmButton = {
                Button(onClick = actions::confirmDiscard, modifier = Modifier.heightIn(min = ActionHeight)) {
                    Text(stringResource(R.string.editor_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = actions::cancelDiscard, modifier = Modifier.heightIn(min = ActionHeight)) {
                    Text(stringResource(R.string.editor_discard_keep))
                }
            },
        )
    }
}

/** The form: the workout section, one card per round, then Add round. Item indexes match [firstItemWithError]. */
@Composable
private fun EditorForm(
    state: EditorState.Editing,
    actions: EditorActions,
    listState: LazyListState,
    focusExerciseId: String?,
    onFocusHandled: () -> Unit,
    onExerciseAdded: (String) -> Unit,
    onRoundAdded: (String) -> Unit,
) {
    val form = state.form
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = WORKOUT_ITEM_KEY) { WorkoutSection(form, state.errors, actions) }
        itemsIndexed(form.rounds, key = { _, round -> round.id }) { index, round ->
            RoundCard(
                round = round,
                position = index,
                roundCount = form.rounds.size,
                form = form,
                errors = state.errors,
                actions = actions,
                isLastField = index == form.rounds.lastIndex,
                focusExerciseId = focusExerciseId,
                onFocusHandled = onFocusHandled,
                onExerciseAdded = onExerciseAdded,
                onRoundAdded = onRoundAdded,
            )
        }
        item(key = ADD_ROUND_ITEM_KEY) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.errors.noRounds) ErrorText(stringResource(R.string.editor_error_no_rounds))
                OutlinedButton(
                    onClick = { actions.addRound()?.let(onRoundAdded) },
                    modifier = Modifier.heightIn(min = ActionHeight),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.editor_add_round))
                }
            }
        }
    }
}

@Composable
private fun NotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.editor_not_found), style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack, modifier = Modifier.heightIn(min = ActionHeight)) { Text(stringResource(R.string.action_back)) }
    }
}

/**
 * The index of the first form item that shows an error: 0 for the workout section, then one per round, then Add
 * round. `null` if there are none.
 */
internal fun firstItemWithError(
    form: WorkoutForm,
    errors: EditorErrors,
): Int? {
    if (errors.workoutNameMissing || DefaultField.entries.any { it.timeField in errors.timeOutOfRange }) return WORKOUT_ITEM
    form.rounds.forEachIndexed { index, round -> if (round.hasError(errors)) return FIRST_ROUND_ITEM + index }
    return if (errors.noRounds) FIRST_ROUND_ITEM + form.rounds.size else null
}

private fun RoundForm.hasError(errors: EditorErrors): Boolean =
    id in errors.roundNameMissing ||
        id in errors.roundHasNoExercises ||
        TimeField.RoundRest(id) in errors.timeOutOfRange ||
        exercises.any { exercise ->
            exercise.id in errors.exerciseNameMissing ||
                TimeField.ExerciseDuration(exercise.id) in errors.timeOutOfRange ||
                TimeField.ExerciseRest(exercise.id) in errors.timeOutOfRange
        }

private val EditorMessage.text: Int
    get() =
        when (this) {
            EditorMessage.CHECK_FIELDS -> R.string.editor_check_fields
            EditorMessage.SAVE_FAILED -> R.string.editor_save_failed
        }

private const val WORKOUT_ITEM = 0
private const val FIRST_ROUND_ITEM = 1
private const val WORKOUT_ITEM_KEY = "workout"
private const val ADD_ROUND_ITEM_KEY = "add-round"
