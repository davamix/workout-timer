package io.github.davamix.workouttimer.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.model.TimeField

// The parts of the editor's form (P6 SPEC §4.3.2–§4.3.4). Fields are tagged for tests: workout-name, default-duration,
// default-exercise-rest, default-round-rest; round-<id> with round-name-<id> and round-rest-<id>; exercise-<id> with
// exercise-name-<id>, exercise-duration-<id> and exercise-rest-<id>.

/** The workout's name and its three defaults: the first item of the form. */
@Composable
internal fun WorkoutSection(
    form: WorkoutForm,
    errors: EditorErrors,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NameField(
            value = form.name,
            onValueChange = { name -> actions.edit { it.withName(name) } },
            label = stringResource(R.string.editor_workout_name),
            missing = errors.workoutNameMissing,
            modifier = Modifier.width(WorkoutNameWidth).testTag("workout-name"),
        )
        Text(stringResource(R.string.editor_defaults), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DefaultField.entries.forEach { field ->
                SecondsField(
                    value = form.default(field),
                    onValueChange = { input -> actions.edit { it.withDefault(field, input) } },
                    label = stringResource(field.label),
                    error = errors.timeOutOfRange[field.timeField],
                    modifier = Modifier.width(DefaultFieldWidth).testTag(field.tag),
                )
            }
        }
    }
}

/**
 * One round: its position, actions, name and own rest, then its exercises as rows of a table, then Add exercise.
 *
 * @param position The round's place in the workout, from 0.
 * @param isLastField Whether the round's last exercise has the form's last field, which gets the Done key.
 * @param focusExerciseId An exercise of this round whose name should take the focus, as after Add exercise.
 */
@Composable
internal fun RoundCard(
    round: RoundForm,
    position: Int,
    roundCount: Int,
    form: WorkoutForm,
    errors: EditorErrors,
    actions: EditorActions,
    isLastField: Boolean,
    focusExerciseId: String?,
    onFocusHandled: () -> Unit,
    onExerciseAdded: (exerciseId: String) -> Unit,
    onRoundAdded: (roundId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastRound = position == roundCount - 1
    Card(modifier.fillMaxWidth().testTag("round-${round.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.editor_round_position, position + 1),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                MoveAndMore(
                    canMoveUp = position > 0,
                    canMoveDown = !isLastRound,
                    onMove = { by -> actions.edit { it.moveRound(round.id, by) } },
                    onDuplicate = { actions.duplicateRound(round.id)?.let(onRoundAdded) },
                    onRemove = { actions.edit { it.removeRound(round.id) } },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NameField(
                    value = round.name,
                    onValueChange = { name -> actions.edit { it.withRoundName(round.id, name) } },
                    label = stringResource(R.string.editor_round_name),
                    missing = round.id in errors.roundNameMissing,
                    modifier = Modifier.width(RoundNameWidth).testTag("round-name-${round.id}"),
                )
                Text(
                    text = stringResource(R.string.editor_round_rest),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 20.dp),
                )
                SecondsField(
                    value = round.rest,
                    onValueChange = { input -> actions.edit { it.withRoundRest(round.id, input) } },
                    defaultValue = form.defaultRoundRest,
                    notUsed = if (isLastRound) stringResource(R.string.editor_not_used_round) else null,
                    error = errors.timeOutOfRange[TimeField.RoundRest(round.id)],
                    modifier = Modifier.width(SecondsFieldWidth).testTag("round-rest-${round.id}"),
                )
            }
            if (round.exercises.isNotEmpty()) ColumnHeaders()
            round.exercises.forEachIndexed { index, exercise ->
                key(exercise.id) {
                    ExerciseRow(
                        exercise = exercise,
                        position = index,
                        exerciseCount = round.exercises.size,
                        form = form,
                        errors = errors,
                        actions = actions,
                        isLastField = isLastField && index == round.exercises.lastIndex,
                        requestFocus = exercise.id == focusExerciseId,
                        onFocusHandled = onFocusHandled,
                    )
                }
            }
            if (round.id in errors.roundHasNoExercises) ErrorText(stringResource(R.string.editor_error_no_exercises))
            OutlinedButton(
                onClick = { actions.addExercise(round.id)?.let(onExerciseAdded) },
                modifier = Modifier.heightIn(min = ActionHeight),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.editor_add_exercise))
            }
        }
    }
}

/** The headers over a round's exercise rows, so the fields themselves need no label. */
@Composable
private fun ColumnHeaders() {
    val style = MaterialTheme.typography.labelLarge
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(RowSpacing)) {
        Spacer(Modifier.width(PositionWidth))
        Text(stringResource(R.string.editor_column_exercise), Modifier.weight(1f), color = color, style = style)
        Text(stringResource(R.string.editor_column_duration), Modifier.width(SecondsFieldWidth), color = color, style = style)
        Text(stringResource(R.string.editor_column_rest), Modifier.width(SecondsFieldWidth), color = color, style = style)
        Spacer(Modifier.width(MoveAndMoreWidth))
    }
}

/** One exercise: its position in the round, name, own duration and own rest, then its actions. */
@Composable
private fun ExerciseRow(
    exercise: ExerciseForm,
    position: Int,
    exerciseCount: Int,
    form: WorkoutForm,
    errors: EditorErrors,
    actions: EditorActions,
    isLastField: Boolean,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
) {
    val id = exercise.id
    val focusRequester = remember { FocusRequester() }
    if (requestFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            onFocusHandled()
        }
    }
    Row(Modifier.fillMaxWidth().testTag("exercise-$id"), horizontalArrangement = Arrangement.spacedBy(RowSpacing)) {
        Text(
            text = (position + 1).toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(PositionWidth).padding(top = 16.dp),
        )
        NameField(
            value = exercise.name,
            onValueChange = { name -> actions.edit { it.withExerciseName(id, name) } },
            missing = id in errors.exerciseNameMissing,
            modifier = Modifier.weight(1f).focusRequester(focusRequester).testTag("exercise-name-$id"),
        )
        SecondsField(
            value = exercise.duration,
            onValueChange = { input -> actions.edit { it.withExerciseDuration(id, input) } },
            defaultValue = form.defaultExerciseDuration,
            error = errors.timeOutOfRange[TimeField.ExerciseDuration(id)],
            modifier = Modifier.width(SecondsFieldWidth).testTag("exercise-duration-$id"),
        )
        SecondsField(
            value = exercise.rest,
            onValueChange = { input -> actions.edit { it.withExerciseRest(id, input) } },
            defaultValue = form.defaultExerciseRest,
            notUsed = if (position == exerciseCount - 1) stringResource(R.string.editor_not_used_exercise) else null,
            error = errors.timeOutOfRange[TimeField.ExerciseRest(id)],
            imeAction = if (isLastField) ImeAction.Done else ImeAction.Next,
            modifier = Modifier.width(SecondsFieldWidth).testTag("exercise-rest-$id"),
        )
        MoveAndMore(
            canMoveUp = position > 0,
            canMoveDown = position < exerciseCount - 1,
            onMove = { by -> actions.edit { it.moveExercise(id, by) } },
            onDuplicate = { actions.duplicateExercise(id) },
            onRemove = { actions.edit { it.removeExercise(id) } },
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** A name field. Shows "Enter a name" when [missing]. */
@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    missing: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        isError = missing,
        supportingText = if (missing) ({ Text(stringResource(R.string.editor_error_name)) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
    )
}

/**
 * A duration or rest in whole seconds (P6 Q3), with a number keyboard. It keeps only digits (see [timeText]).
 *
 * @param label A floating label, for the workout defaults. Own-value fields have none, so the default shows.
 * @param defaultValue For an own value: the default it uses while empty. It shows as a dimmed placeholder with
 *   "Default" under it; once a value is typed, a "Use default" button clears it (§4.3.2).
 * @param notUsed For an unused rest: the text that says so. The field is dimmed but stays editable (§4.3.2).
 * @param error The range the value must be in, when it's outside it.
 */
@Composable
internal fun SecondsField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    defaultValue: String? = null,
    notUsed: String? = null,
    error: IntRange? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val supporting =
        when {
            error != null -> stringResource(R.string.editor_error_range, error.first, error.last)
            notUsed != null -> notUsed
            defaultValue != null && value.isEmpty() -> stringResource(R.string.editor_uses_default)
            else -> null
        }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(timeText(it)) },
        modifier = modifier.alpha(if (notUsed != null && error == null) NOT_USED_ALPHA else 1f),
        label = label?.let { { Text(it) } },
        placeholder =
            if (defaultValue != null && defaultValue.isNotEmpty()) {
                // Dimmer than a typed value, so a linked value doesn't look like an own one.
                { Text(defaultValue, Modifier.alpha(PLACEHOLDER_ALPHA)) }
            } else {
                null
            },
        suffix = { Text(stringResource(R.string.editor_seconds)) },
        trailingIcon =
            if (defaultValue != null && value.isNotEmpty()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.editor_use_default))
                    }
                }
            } else {
                null
            },
        supportingText = supporting?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
    )
}

/** Move up, Move down, and a menu with Duplicate and Remove: for a round or an exercise (§4.3.3). */
@Composable
private fun MoveAndMore(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (by: Int) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(modifier.width(MoveAndMoreWidth)) {
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.editor_move_up))
        }
        IconButton(onClick = { onMove(+1) }, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.editor_move_down))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.editor_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.editor_duplicate)) },
                    onClick = {
                        menuOpen = false
                        onDuplicate()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.editor_remove)) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}

/** An error that belongs to no single field, such as a round without exercises. */
@Composable
internal fun ErrorText(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
}

private fun WorkoutForm.default(field: DefaultField): String =
    when (field) {
        DefaultField.EXERCISE_DURATION -> defaultExerciseDuration
        DefaultField.EXERCISE_REST -> defaultExerciseRest
        DefaultField.ROUND_REST -> defaultRoundRest
    }

private val DefaultField.label: Int
    get() =
        when (this) {
            DefaultField.EXERCISE_DURATION -> R.string.editor_default_duration
            DefaultField.EXERCISE_REST -> R.string.editor_default_exercise_rest
            DefaultField.ROUND_REST -> R.string.editor_default_round_rest
        }

/** The validation field each default is reported under. */
internal val DefaultField.timeField: TimeField
    get() =
        when (this) {
            DefaultField.EXERCISE_DURATION -> TimeField.DefaultExerciseDuration
            DefaultField.EXERCISE_REST -> TimeField.DefaultExerciseRest
            DefaultField.ROUND_REST -> TimeField.DefaultRoundRest
        }

private val DefaultField.tag: String
    get() =
        when (this) {
            DefaultField.EXERCISE_DURATION -> "default-duration"
            DefaultField.EXERCISE_REST -> "default-exercise-rest"
            DefaultField.ROUND_REST -> "default-round-rest"
        }

/** Height of buttons that aren't in the top bar: large enough to hit from arm's length (NF-6). */
internal val ActionHeight = 56.dp

private val WorkoutNameWidth = 480.dp
private val DefaultFieldWidth = 260.dp
private val RoundNameWidth = 360.dp
private val SecondsFieldWidth = 200.dp
private val PositionWidth = 32.dp
private val RowSpacing = 12.dp

/** Three 48 dp icon buttons. */
private val MoveAndMoreWidth = 144.dp

private const val NOT_USED_ALPHA = 0.6f
private const val PLACEHOLDER_ALPHA = 0.6f
