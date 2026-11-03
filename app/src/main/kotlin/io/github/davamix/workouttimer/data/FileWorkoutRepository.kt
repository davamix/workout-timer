package io.github.davamix.workouttimer.data

import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Extension of a stored workout's file: `<id>.json`. */
private const val JSON_SUFFIX = ".json"

/** IDs that can be used as file names: no `/`, no `.`, so they can never point outside the directory. */
private val validWorkoutId = Regex("[A-Za-z0-9_-]{1,64}")

/**
 * Whether [id] can be stored by [FileWorkoutRepository]. UUIDs from
 * [io.github.davamix.workouttimer.core.model.newId] always can.
 */
internal fun isValidWorkoutId(id: String): Boolean = validWorkoutId.matches(id)

/**
 * A [WorkoutRepository] that stores each workout as `<id>.json` in [directory], in [WorkoutFileFormat] (plan D-4).
 *
 * - **Loading** happens once, on first use: the first collection of [workouts], or the first [get], [save] or
 *   [delete]. Leftover temporary files from an interrupted save are deleted. A damaged file is skipped and reported
 *   to [onWarning], and left as it is; the other workouts load normally.
 * - **Saving** is atomic ([writeAtomically]): if it fails, the old file and the in-memory list are unchanged.
 * - **Concurrency:** one lock guards loading, saving and deleting, so operations apply in call order. Only one
 *   instance may use a directory at a time; the app's `AppContainer` holds the only one.
 *
 * Like [io.github.davamix.workouttimer.core.repository.InMemoryWorkoutRepository], it doesn't validate workouts, and
 * [workouts] behaves like a state flow.
 *
 * @param directory Where the files live: `<filesDir>/workouts` in the app. Created if missing.
 * @param ioDispatcher Where file work runs. Tests pass a test dispatcher.
 * @param onWarning Called with a message and the error for every problem that doesn't stop the repository, such as a
 *   damaged file. The app logs it in debug builds only.
 */
class FileWorkoutRepository(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onWarning: (message: String, error: Throwable) -> Unit = { _, _ -> },
) : WorkoutRepository {
    private val mutex = Mutex()

    /** Saved workouts by ID, or `null` until [directory] has been loaded. */
    private val byId = MutableStateFlow<Map<String, Workout>?>(null)

    override val workouts: Flow<List<Workout>> =
        flow {
            loaded()
            emitAll(byId.filterNotNull().map { it.values.toList() })
        }

    override suspend fun get(id: String): Workout? = loaded()[id]

    /**
     * Saves [workout] to `<id>.json`, replacing the file if there is one.
     *
     * @throws IllegalArgumentException if the workout's ID can't be a file name (see [isValidWorkoutId]).
     * @throws IOException if the file can't be written. The stored workout, if any, is unchanged.
     */
    override suspend fun save(workout: Workout) {
        require(isValidWorkoutId(workout.id)) { "A workout ID must match ${validWorkoutId.pattern}: ${workout.id}" }
        val bytes = WorkoutFileFormat.encode(workout).toByteArray(Charsets.UTF_8)
        mutex.withLock {
            val current = loadLocked()
            withContext(ioDispatcher) {
                if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Can't create $directory")
                writeAtomically(fileFor(workout.id)) { it.write(bytes) }
                // Updated here, with no suspension point after the move, so a cancelled caller can't leave the list
                // out of step with the files.
                byId.value = current + (workout.id to workout)
            }
        }
    }

    /**
     * Deletes the workout's file. Does nothing if there is none, or if [id] can't be a file name.
     *
     * @throws IOException if the file exists but can't be deleted. The workout stays listed.
     */
    override suspend fun delete(id: String) {
        if (!isValidWorkoutId(id)) return
        mutex.withLock {
            val current = loadLocked()
            withContext(ioDispatcher) {
                val file = fileFor(id)
                if (file.exists() && !file.delete()) throw IOException("Can't delete ${file.name}")
                byId.value = current - id
            }
        }
    }

    private fun fileFor(id: String) = File(directory, id + JSON_SUFFIX)

    /** The saved workouts, loading them on first use. */
    private suspend fun loaded(): Map<String, Workout> = byId.value ?: mutex.withLock { loadLocked() }

    /** The saved workouts, loading them on first use. Call only while holding [mutex]. */
    private suspend fun loadLocked(): Map<String, Workout> =
        byId.value ?: withContext(ioDispatcher) { readDirectory() }.also { byId.value = it }

    private fun readDirectory(): Map<String, Workout> {
        if (!directory.isDirectory && !directory.mkdirs()) {
            onWarning("Can't create the workouts directory $directory", IOException("mkdirs failed"))
            return emptyMap()
        }
        val files = directory.listFiles()
        if (files == null) {
            onWarning("Can't list the workouts directory $directory", IOException("listFiles failed"))
            return emptyMap()
        }

        // Left by a save that was interrupted. The file it was replacing, if any, is intact.
        files.filter { it.isFile && it.name.endsWith(JSON_SUFFIX + TEMP_SUFFIX) }.forEach { it.delete() }

        return files
            .filter { it.isFile && it.name.endsWith(JSON_SUFFIX) && isValidWorkoutId(it.name.removeSuffix(JSON_SUFFIX)) }
            .mapNotNull { readWorkout(it) }
            .associateBy { it.id }
    }

    /** The workout in [file], or `null` if the file is damaged. Damaged files are reported, never changed. */
    private fun readWorkout(file: File): Workout? =
        try {
            val workout = WorkoutFileFormat.decode(file.readText(Charsets.UTF_8))
            val expectedId = file.name.removeSuffix(JSON_SUFFIX)
            if (workout.id != expectedId) {
                throw WorkoutFormatException("The ID inside, ${workout.id}, doesn't match the file name")
            }
            workout
        } catch (e: IOException) {
            onWarning("Skipped workout file ${file.name}: can't read it", e)
            null
        } catch (e: WorkoutFormatException) {
            onWarning("Skipped damaged workout file ${file.name}: ${e.message}", e)
            null
        }
}
