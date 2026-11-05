package io.github.davamix.workouttimer.ui

import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.repository.InMemoryWorkoutRepository
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.settings.InMemorySettingsRepository
import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.core.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.io.IOException

/**
 * An in-memory [WorkoutRepository] for ViewModel tests. It can hold back its data, as P3's repository does while it
 * loads the files, and its writes can be made to fail as P3's do: with an [IOException], keeping the old data.
 *
 * @param loaded Whether the data is available from the start. If not, [workouts] emits nothing and [get] waits until
 *   [finishLoading] is called.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestWorkoutRepository(
    initial: List<Workout> = emptyList(),
    loaded: Boolean = true,
) : WorkoutRepository {
    private val delegate = InMemoryWorkoutRepository(initial)
    private val ready = MutableStateFlow(loaded)

    /** While `true`, [save] and [delete] throw [IOException] and change nothing. */
    var failWrites = false

    /** How many times [save] was called, including failed calls. */
    var saveCalls = 0
        private set

    override val workouts: Flow<List<Workout>> = ready.flatMapLatest { if (it) delegate.workouts else emptyFlow() }

    override suspend fun get(id: String): Workout? {
        ready.first { it }
        return delegate.get(id)
    }

    override suspend fun save(workout: Workout) {
        saveCalls++
        if (failWrites) throw IOException("Test: save failed")
        delegate.save(workout)
    }

    override suspend fun delete(id: String) {
        if (failWrites) throw IOException("Test: delete failed")
        delegate.delete(id)
    }

    /** Makes the data available, as when P3's repository has read the files. */
    fun finishLoading() {
        ready.value = true
    }

    /** Every saved workout, sorted by ID, read without going through the ViewModel. */
    suspend fun all(): List<Workout> = delegate.workouts.first().sortedBy { it.id }
}

/** An in-memory [SettingsRepository] for ViewModel tests, whose writes can be made to fail with an [IOException]. */
class TestSettingsRepository(
    initial: Settings = Settings(),
) : SettingsRepository {
    private val delegate = InMemorySettingsRepository(initial)

    /** While `true`, [setVoiceCuesEnabled] throws [IOException] and changes nothing. */
    var failWrites = false

    override val settings: Flow<Settings> = delegate.settings

    override suspend fun setVoiceCuesEnabled(enabled: Boolean) {
        if (failWrites) throw IOException("Test: write failed")
        delegate.setVoiceCuesEnabled(enabled)
    }
}
