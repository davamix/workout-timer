package io.github.davamix.workouttimer.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.newId
import io.github.davamix.workouttimer.core.model.validate
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.timeline.estimatedTotalSeconds
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * The workout editor (P6 SPEC §4.3.2–§4.3.5): it opens a new or saved workout as a [WorkoutForm], applies the edits,
 * keeps the total up to date (WM-6), validates and saves (WM-7, WM-4), and asks before discarding changes (WM-10).
 *
 * @param workoutId The saved workout to edit, or `null` for a new one.
 * @param roundName The name of a new round from its number: "Round %1$d" from the resources.
 * @param newId Source of IDs for new workouts, rounds and exercises; tests pass a deterministic one.
 */
class WorkoutEditorViewModel(
    private val repository: WorkoutRepository,
    private val workoutId: String?,
    private val roundName: (number: Int) -> String,
    private val newId: () -> String = ::newId,
) : ViewModel() {
    /**
     * What the editor shows. It's Compose snapshot state, changed synchronously by every edit, so text fields get their
     * new value in the same frame (§4.3.5). Read it during composition, or directly in a test.
     */
    var state: EditorState by mutableStateOf(EditorState.Loading)
        private set

    /** The workout as opened, mapped like the form, to tell whether anything changed. */
    private var baseline: Workout? = null

    /** Whether Save has been pressed once: from then on, errors show and follow every edit (P6 Q1). */
    private var errorsShown = false

    init {
        if (workoutId == null) {
            open(newWorkoutForm(roundName(1), newId), isNew = true)
        } else {
            viewModelScope.launch {
                val workout = repository.get(workoutId)
                if (workout == null) state = EditorState.NotFound else open(workout.toForm(), isNew = false)
            }
        }
    }

    private fun open(
        form: WorkoutForm,
        isNew: Boolean,
    ) {
        val workout = form.toWorkout()
        baseline = workout
        state =
            EditorState.Editing(
                form = form,
                isNew = isNew,
                totalSeconds = workout.estimatedTotalSeconds(),
                errors = EditorErrors.NONE,
                hasChanges = false,
            )
    }

    /** Applies [change] to the form. Ignored unless editing, and while a save runs. */
    fun edit(change: (WorkoutForm) -> WorkoutForm) {
        val current = state as? EditorState.Editing ?: return
        if (current.saving) return
        val form = change(current.form)
        if (form == current.form) return
        val workout = form.toWorkout()
        state =
            current.copy(
                form = form,
                totalSeconds = workout.estimatedTotalSeconds(),
                errors = if (errorsShown) workout.validate().toEditorErrors() else EditorErrors.NONE,
                hasChanges = workout != baseline,
            )
    }

    /** Adds a round called "Round *n*" at the end, with one exercise to name. Returns the new round's ID. */
    fun addRound(): String? = added { form, ids -> form.addRound(roundName(form.rounds.size + 1), ids) }

    /** Adds an exercise to name at the end of the round [roundId]. Returns the new exercise's ID. */
    fun addExercise(roundId: String): String? = added { form, ids -> form.addExercise(roundId, ids) }

    /** Puts a copy of the round [roundId] right after it. Returns the copy's ID. */
    fun duplicateRound(roundId: String): String? = added { form, ids -> form.duplicateRound(roundId, ids) }

    /** Puts a copy of the exercise [exerciseId] right after it. Returns the copy's ID. */
    fun duplicateExercise(exerciseId: String): String? = added { form, ids -> form.duplicateExercise(exerciseId, ids) }

    /**
     * Applies an operation that adds a round or an exercise, and returns the ID of the round or exercise it added: the
     * first ID it asked for. `null` if nothing was added.
     */
    private fun added(operation: (WorkoutForm, () -> String) -> WorkoutForm): String? {
        var first: String? = null
        val ids = {
            val id = newId()
            if (first == null) first = id
            id
        }
        val before = (state as? EditorState.Editing)?.form ?: return null
        edit { operation(it, ids) }
        return first.takeIf { (state as? EditorState.Editing)?.form != before }
    }

    /**
     * Validates the form and saves it if it's valid; the editor then closes (P6 Q5). Otherwise nothing is saved, the
     * errors show from now on, and the message says to check them. If saving fails, the form is kept and the message
     * says so. Does nothing while a save runs.
     */
    fun save() {
        val current = state as? EditorState.Editing ?: return
        if (current.saving) return
        val workout = current.form.toWorkout()
        val errors = workout.validate().toEditorErrors()
        errorsShown = true
        if (!errors.isEmpty) {
            state = current.copy(errors = errors, message = EditorMessage.CHECK_FIELDS)
            return
        }
        state = current.copy(errors = errors, saving = true)
        viewModelScope.launch {
            state =
                try {
                    repository.save(workout)
                    EditorState.Closed
                } catch (_: IOException) {
                    (state as? EditorState.Editing)?.copy(saving = false, message = EditorMessage.SAVE_FAILED) ?: state
                }
        }
    }

    /**
     * Cancel, the back arrow and system Back. With unsaved changes it asks first (WM-10); otherwise the editor closes.
     * Ignored while a save runs: the save closes the editor.
     */
    fun requestClose() {
        val current = state
        state =
            when {
                current !is EditorState.Editing -> EditorState.Closed
                current.saving -> current
                current.hasChanges -> current.copy(confirmingDiscard = true)
                else -> EditorState.Closed
            }
    }

    /** Leaves without saving. */
    fun confirmDiscard() {
        if (state is EditorState.Editing) state = EditorState.Closed
    }

    /** Keeps editing: closes the discard confirmation. */
    fun cancelDiscard() {
        val current = state as? EditorState.Editing ?: return
        state = current.copy(confirmingDiscard = false)
    }

    /** The screen has shown [EditorState.Editing.message]. */
    fun messageShown() {
        val current = state as? EditorState.Editing ?: return
        state = current.copy(message = null)
    }

    companion object {
        /** Builds the ViewModel for the workout [workoutId], or a new one, with the app's workout repository. */
        fun factory(workoutId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as WorkoutTimerApp
                    WorkoutEditorViewModel(
                        repository = app.container.workoutRepository,
                        workoutId = workoutId,
                        roundName = { app.getString(R.string.editor_round_default_name, it) },
                    )
                }
            }
    }
}

/** What the editor shows. */
sealed interface EditorState {
    /** The saved workout is being read. */
    data object Loading : EditorState

    /** No saved workout has the ID the editor was opened with. */
    data object NotFound : EditorState

    /**
     * The form, ready to edit.
     *
     * @property isNew Whether this is a new workout, for the title.
     * @property totalSeconds The total from Start to completion (WM-6), kept up to date with every edit.
     * @property errors The validation errors to show: none until the first Save attempt (P6 Q1).
     * @property hasChanges Whether the workout differs from the one the editor opened with (§4.3.5).
     * @property confirmingDiscard Whether the "Discard changes?" dialog is open.
     * @property saving Whether a save is running.
     * @property message A message to show once, in a snackbar.
     */
    data class Editing(
        val form: WorkoutForm,
        val isNew: Boolean,
        val totalSeconds: Int,
        val errors: EditorErrors,
        val hasChanges: Boolean,
        val confirmingDiscard: Boolean = false,
        val saving: Boolean = false,
        val message: EditorMessage? = null,
    ) : EditorState

    /** The editor is done, after saving or after leaving without saving: the screen calls `onClose` once. */
    data object Closed : EditorState
}

/** Messages the editor shows in a snackbar. */
enum class EditorMessage { CHECK_FIELDS, SAVE_FAILED }
