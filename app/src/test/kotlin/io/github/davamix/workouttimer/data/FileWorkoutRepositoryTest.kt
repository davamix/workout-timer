package io.github.davamix.workouttimer.data

import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.duplicate
import io.github.davamix.workouttimer.core.model.newId
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class FileWorkoutRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    /** The repository's directory. Not created up front: the repository creates it. */
    private val dir by lazy { File(tempDir, "workouts") }

    private val warnings = mutableListOf<Pair<String, Throwable>>()

    private val legs = workout(round("R", exercise("Squats")), name = "Legs")
    private val arms = workout(round("R", exercise("Push-ups")), name = "Arms")

    private fun TestScope.repository() =
        FileWorkoutRepository(dir, StandardTestDispatcher(testScheduler)) { message, error -> warnings += message to error }

    /** The order of [WorkoutRepository.workouts] isn't specified, so lists are compared as sets. */
    private suspend fun WorkoutRepository.saved(): Set<Workout> = workouts.first().toSet()

    private fun fileOf(id: String) = File(dir, "$id.json")

    /** Writes [text] straight into the directory, as if left there by an earlier run or by hand. */
    private fun putFile(
        name: String,
        text: String,
    ): File {
        dir.mkdirs()
        return File(dir, name).apply { writeText(text) }
    }

    /** Files in the directory, by name. */
    private fun fileNames(): Set<String> = dir.list()?.toSet() ?: emptySet()

    @Test
    fun `a new repository on a missing directory is empty and creates the directory`() =
        runTest {
            val repository = repository()

            assertEquals(emptySet<Workout>(), repository.saved())
            assertNull(repository.get(legs.id))
            assertTrue(dir.isDirectory)
            assertEquals(emptyList<Pair<String, Throwable>>(), warnings)
        }

    @Test
    fun `a saved workout can be read back, and is stored as its own file`() =
        runTest {
            val repository = repository()

            repository.save(exampleWorkout)

            assertEquals(exampleWorkout, repository.get(exampleWorkout.id))
            assertEquals(setOf(exampleWorkout), repository.saved())
            assertEquals(setOf("w-example.json"), fileNames())
            assertEquals(exampleWorkout, WorkoutFileFormat.decode(fileOf(exampleWorkout.id).readText()))
        }

    @Test
    fun `several workouts are all listed`() =
        runTest {
            val repository = repository()

            repository.save(legs)
            repository.save(arms)
            repository.save(exampleWorkout)

            assertEquals(setOf(legs, arms, exampleWorkout), repository.saved())
            assertEquals(setOf("w-legs.json", "w-arms.json", "w-example.json"), fileNames())
        }

    @Test
    fun `saving a workout with a known ID replaces it`() =
        runTest {
            val repository = repository()
            repository.save(legs)
            repository.save(arms)
            val edited = legs.copy(name = "Leg day", rounds = legs.rounds + round("Extra", exercise("Lunges")))

            repository.save(edited)

            assertEquals(edited, repository.get(legs.id))
            assertEquals(setOf(edited, arms), repository.saved())
            assertEquals(edited, WorkoutFileFormat.decode(fileOf(legs.id).readText()))
        }

    @Test
    fun `a duplicate is stored as a separate workout`() =
        runTest {
            val repository = repository()
            repository.save(exampleWorkout)
            val copy = exampleWorkout.duplicate("Example (copy)")

            repository.save(copy)

            assertEquals(setOf(exampleWorkout, copy), repository.saved())
            assertTrue(fileOf(copy.id).isFile)
            assertTrue(fileOf(exampleWorkout.id).isFile)
        }

    @Test
    fun `a deleted workout is gone, and so is its file`() =
        runTest {
            val repository = repository()
            repository.save(legs)
            repository.save(arms)

            repository.delete(legs.id)

            assertNull(repository.get(legs.id))
            assertEquals(setOf(arms), repository.saved())
            assertEquals(setOf("w-arms.json"), fileNames())
        }

    @Test
    fun `deleting an unknown ID does nothing`() =
        runTest {
            val repository = repository()
            repository.save(legs)

            repository.delete("unknown")

            assertEquals(setOf(legs), repository.saved())
            assertEquals(setOf("w-legs.json"), fileNames())
        }

    @Test
    fun `a new instance on the same directory loads every saved workout`() =
        runTest {
            val first = repository()
            first.save(legs)
            first.save(arms)
            first.save(exampleWorkout)
            first.delete(arms.id)

            val afterRestart = repository()

            assertEquals(setOf(legs, exampleWorkout), afterRestart.saved())
            assertEquals(exampleWorkout, afterRestart.get(exampleWorkout.id))
        }

    @Test
    fun `damaged files are skipped, reported and left untouched, while the others load`() =
        runTest {
            putFile("w-legs.json", WorkoutFileFormat.encode(legs))
            val damaged =
                mapOf(
                    "truncated.json" to WorkoutFileFormat.encode(arms).take(40),
                    "empty.json" to "",
                    "newer.json" to """{"schemaVersion": 2}""",
                    "wrong-id.json" to WorkoutFileFormat.encode(arms),
                ).mapValues { (name, text) -> putFile(name, text) to text }

            val repository = repository()

            assertEquals(setOf(legs), repository.saved())
            assertEquals(damaged.keys, warnings.map { (message, _) -> damaged.keys.single { it in message } }.toSet())
            assertEquals(damaged.size, warnings.size)
            assertTrue(warnings.all { (message, _) -> message.startsWith("Skipped damaged workout file") })
            for ((file, text) in damaged.values) assertEquals(text, file.readText(), "${file.name} is left as it was")
        }

    @Test
    fun `a file whose ID doesn't match its name is reported as such`() =
        runTest {
            putFile("w-other.json", WorkoutFileFormat.encode(legs))

            val repository = repository()

            assertEquals(emptySet<Workout>(), repository.saved())
            val (message, error) = warnings.single()
            assertTrue("w-other.json" in message, message)
            assertTrue("doesn't match the file name" in message, message)
            assertInstanceOf(WorkoutFormatException::class.java, error)
        }

    @Test
    fun `leftover temporary files are deleted, and other files are ignored and kept`() =
        runTest {
            putFile("w-legs.json", WorkoutFileFormat.encode(legs))
            putFile("w-legs.json.tmp", "{ half a workout")
            putFile("w-new.json.tmp", "{ a new workout that was never moved into place")
            putFile("notes.txt", "not a workout")
            putFile("bad id.json", WorkoutFileFormat.encode(legs.copy(id = "bad id")))
            File(dir, "folder.json").mkdir()

            val repository = repository()

            assertEquals(setOf(legs), repository.saved())
            assertEquals(setOf("w-legs.json", "notes.txt", "bad id.json", "folder.json"), fileNames())
            assertEquals(emptyList<Pair<String, Throwable>>(), warnings)
        }

    @Test
    fun `an ID that can't be a file name is rejected by save and ignored by get and delete`() =
        runTest {
            val repository = repository()
            repository.save(legs)
            val badIds = listOf("", "../escape", "a/b", "a\\b", "dot.ted", "with space", "x".repeat(65))

            for (id in badIds) {
                val error = runCatching { repository.save(legs.copy(id = id)) }.exceptionOrNull()
                assertInstanceOf(IllegalArgumentException::class.java, error, "save(\"$id\")")
                assertNull(repository.get(id))
                repository.delete(id)
            }

            assertEquals(setOf(legs), repository.saved())
            assertEquals(setOf("w-legs.json"), fileNames())
            assertEquals(setOf("workouts"), tempDir.list()!!.toSet())
        }

    @Test
    fun `valid IDs include the app's UUIDs and stop at 64 characters`() {
        assertTrue(isValidWorkoutId(newId()))
        assertTrue(isValidWorkoutId("Az09_-"))
        assertTrue(isValidWorkoutId("x".repeat(64)))
        assertFalse(isValidWorkoutId("x".repeat(65)))
        assertFalse(isValidWorkoutId(""))
    }

    @Test
    fun `a failed save throws and leaves the files and the list unchanged`() =
        runTest {
            val repository = repository()
            repository.save(legs)
            val newWorkout = arms
            // A directory where the temporary file would go makes the write fail.
            tempFileFor(fileOf(legs.id)).mkdir()
            tempFileFor(fileOf(newWorkout.id)).mkdir()

            val replaceError = runCatching { repository.save(legs.copy(name = "Leg day")) }.exceptionOrNull()
            val insertError = runCatching { repository.save(newWorkout) }.exceptionOrNull()

            assertInstanceOf(IOException::class.java, replaceError)
            assertInstanceOf(IOException::class.java, insertError)
            assertEquals(legs, repository.get(legs.id))
            assertEquals(setOf(legs), repository.saved())
            assertEquals(legs, WorkoutFileFormat.decode(fileOf(legs.id).readText()))
            assertFalse(fileOf(newWorkout.id).exists())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `workouts emits after each change, and not after a no-op`() =
        runTest {
            val repository = repository()
            val emissions = mutableListOf<List<Workout>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.workouts.toList(emissions) }
            val renamed = legs.copy(name = "Leg day")

            repository.save(legs)
            repository.save(arms)
            repository.save(arms)
            repository.save(renamed)
            repository.delete("unknown")
            repository.delete(legs.id)

            assertEquals(
                listOf(emptySet(), setOf(legs), setOf(legs, arms), setOf(renamed, arms), setOf(arms)),
                emissions.map { it.toSet() },
            )
        }
}
