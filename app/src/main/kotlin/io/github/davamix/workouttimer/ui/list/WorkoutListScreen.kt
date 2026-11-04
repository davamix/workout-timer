package io.github.davamix.workouttimer.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.ui.common.formatTotalDuration

/** The workout list, the home screen (P6 SPEC §4.3.1). */
@Composable
fun WorkoutListScreen(
    onNewWorkout: () -> Unit,
    onEditWorkout: (workoutId: String) -> Unit,
    onStartWorkout: (workoutId: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WorkoutListViewModel = viewModel(factory = WorkoutListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    WorkoutListContent(
        state = state,
        onNewWorkout = onNewWorkout,
        onEditWorkout = onEditWorkout,
        onStartWorkout = onStartWorkout,
        onOpenSettings = onOpenSettings,
        onDuplicate = { item -> viewModel.duplicate(item.id, resources.getString(R.string.list_copy_name, item.name)) },
        onDelete = { item -> viewModel.requestDelete(item.id) },
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onMessageShown = viewModel::messageShown,
    )
}

/** Draws [state]. Every action goes to a callback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListContent(
    state: WorkoutListState,
    onNewWorkout: () -> Unit,
    onEditWorkout: (workoutId: String) -> Unit,
    onStartWorkout: (workoutId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onDuplicate: (WorkoutListItem) -> Unit,
    onDelete: (WorkoutListItem) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(it.text) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title)) },
                actions = {
                    Button(onClick = onNewWorkout, modifier = Modifier.heightIn(min = MainActionHeight)) {
                        ButtonIcon(Icons.Filled.Add)
                        Text(stringResource(R.string.list_new_workout))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.list_settings))
                    }
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.workouts.isEmpty() ->
                    Text(
                        text = stringResource(R.string.list_empty),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                else ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(state.workouts, key = { it.id }) { item ->
                            WorkoutRow(
                                item = item,
                                onStart = { onStartWorkout(item.id) },
                                onEdit = { onEditWorkout(item.id) },
                                onDuplicate = { onDuplicate(item) },
                                onDelete = { onDelete(item) },
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }

    state.pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.list_delete_title, item.name)) },
            text = { Text(stringResource(R.string.list_delete_text)) },
            confirmButton = {
                Button(onClick = onConfirmDelete, modifier = Modifier.heightIn(min = MainActionHeight)) {
                    Text(stringResource(R.string.list_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete, modifier = Modifier.heightIn(min = MainActionHeight)) {
                    Text(stringResource(R.string.list_delete_cancel))
                }
            },
        )
    }
}

/** A workout: its name and total, then Start, Edit, Duplicate and Delete. Tagged `workout-<id>` for tests. */
@Composable
private fun WorkoutRow(
    item: WorkoutListItem,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).testTag("workout-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.list_total, formatTotalDuration(item.totalSeconds)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onStart, modifier = Modifier.heightIn(min = MainActionHeight)) {
            ButtonIcon(Icons.Filled.PlayArrow)
            Text(stringResource(R.string.list_start))
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = MainActionHeight)) {
            ButtonIcon(Icons.Filled.Edit)
            Text(stringResource(R.string.list_edit))
        }
        OutlinedButton(onClick = onDuplicate, modifier = Modifier.heightIn(min = MainActionHeight)) {
            Text(stringResource(R.string.list_duplicate))
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.heightIn(min = MainActionHeight)) {
            ButtonIcon(Icons.Filled.Delete)
            Text(stringResource(R.string.list_delete))
        }
    }
}

/** An icon before a button's label. Decorative: the label says what the button does. */
@Composable
private fun ButtonIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
}

/** Height of the main actions (P6 SPEC §4.3.8, NF-6). */
private val MainActionHeight = 56.dp

private val ListMessage.text: Int
    get() =
        when (this) {
            ListMessage.DUPLICATE_FAILED -> R.string.list_duplicate_failed
            ListMessage.DELETE_FAILED -> R.string.list_delete_failed
        }
