package io.github.davamix.workouttimer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.duplicate
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.timeline.estimatedTotalSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * The workout list (P6 SPEC §4.3.1): every saved workout with its total (WM-3, WM-6), and the actions that change
 * them: duplicate, and delete after a confirmation (WM-4, WM-9).
 */
class WorkoutListViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val pendingDeleteId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<ListMessage?>(null)

    /** What the list shows. It starts loading and stays so until the repository's first list arrives. */
    val state: StateFlow<WorkoutListState> =
        combine(
            repository.workouts
                .map<List<Workout>, List<WorkoutListItem>?> { workouts -> workouts.sortedWith(listOrder).map { it.toItem() } }
                .onStart { emit(null) },
            pendingDeleteId,
            message,
        ) { items, deleteId, message ->
            if (items == null) {
                WorkoutListState(loading = true, message = message)
            } else {
                WorkoutListState(
                    loading = false,
                    workouts = items,
                    pendingDelete = items.find { it.id == deleteId },
                    message = message,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WorkoutListState())

    /**
     * Saves a copy of the workout [workoutId] called [copyName], with new IDs for it and for every round and exercise.
     * The screen formats [copyName] from a resource. If saving fails, [WorkoutListState.message] says so.
     */
    fun duplicate(
        workoutId: String,
        copyName: String,
    ) {
        viewModelScope.launch {
            try {
                val workout = repository.get(workoutId) ?: return@launch
                repository.save(workout.duplicate(copyName))
            } catch (_: IOException) {
                message.value = ListMessage.DUPLICATE_FAILED
            }
        }
    }

    /** Opens the delete confirmation for the workout [workoutId] (WM-9). */
    fun requestDelete(workoutId: String) {
        pendingDeleteId.value = workoutId
    }

    /** Deletes the workout the confirmation is open for, and closes it. If deleting fails, the message says so. */
    fun confirmDelete() {
        val id = pendingDeleteId.value ?: return
        pendingDeleteId.value = null
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (_: IOException) {
                message.value = ListMessage.DELETE_FAILED
            }
        }
    }

    /** Closes the delete confirmation and keeps the workout. */
    fun cancelDelete() {
        pendingDeleteId.value = null
    }

    /** The screen has shown [WorkoutListState.message]. */
    fun messageShown() {
        message.value = null
    }

    companion object {
        /** Builds the ViewModel with the app's workout repository. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer { WorkoutListViewModel((this[APPLICATION_KEY] as WorkoutTimerApp).container.workoutRepository) }
            }

        /** By name ignoring case, then by ID, so workouts with the same name keep a stable order (P6 Q4). */
        private val listOrder: Comparator<Workout> =
            compareBy<Workout, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id }

        private fun Workout.toItem() = WorkoutListItem(id = id, name = name, totalSeconds = estimatedTotalSeconds())
    }
}

/**
 * What the workout list shows.
 *
 * @property loading `true` until the repository's first list arrives.
 * @property workouts Every saved workout, sorted by name.
 * @property pendingDelete The workout whose delete confirmation is open, if any.
 * @property message A failure to show once, in a snackbar.
 */
data class WorkoutListState(
    val loading: Boolean = true,
    val workouts: List<WorkoutListItem> = emptyList(),
    val pendingDelete: WorkoutListItem? = null,
    val message: ListMessage? = null,
)

/** One row of the workout list. [totalSeconds] runs from Start to completion (WM-6). */
data class WorkoutListItem(
    val id: String,
    val name: String,
    val totalSeconds: Int,
)

/** Failures the list reports in a snackbar. */
enum class ListMessage { DUPLICATE_FAILED, DELETE_FAILED }
