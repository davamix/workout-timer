package io.github.davamix.workouttimer.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.newId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * [FileWorkoutRepository] on the tablet's own file system (WM-8): files written, atomically moved and read back by a
 * new instance. Runs in a scratch directory under the app's `filesDir`, removed afterwards.
 */
@RunWith(AndroidJUnit4::class)
class FileWorkoutRepositoryDeviceTest {
    private val filesDir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
    private val dir = File(filesDir, "test-workouts-${newId()}")

    private val workout =
        Workout(
            id = newId(),
            name = "Device test",
            defaultExerciseDurationSeconds = 40,
            defaultExerciseRestSeconds = 15,
            defaultRoundRestSeconds = 60,
            rounds =
                listOf(
                    Round(
                        id = newId(),
                        name = "Warm-up",
                        restSeconds = 30,
                        exercises =
                            listOf(
                                Exercise(id = newId(), name = "Jumping jacks", durationSeconds = 30),
                                Exercise(id = newId(), name = "Arm circles", durationSeconds = 10, restSeconds = 5),
                            ),
                    ),
                    Round(id = newId(), name = "Core", exercises = listOf(Exercise(id = newId(), name = "Crunches"))),
                ),
        )

    @After
    fun deleteScratchDirectory() {
        dir.deleteRecursively()
    }

    @Test
    fun savedWorkoutIsLoadedByANewInstance() =
        runBlocking {
            FileWorkoutRepository(dir).save(workout)

            val afterRestart = FileWorkoutRepository(dir)

            assertEquals(workout, afterRestart.get(workout.id))
            assertEquals(listOf(workout), afterRestart.workouts.first())
        }

    @Test
    fun saveReplacesTheFileAndDeleteRemovesIt() =
        runBlocking {
            val repository = FileWorkoutRepository(dir)
            repository.save(workout)
            val edited = workout.copy(name = "Device test, edited", defaultExerciseRestSeconds = 20)

            repository.save(edited)

            assertEquals(listOf("${workout.id}.json"), dir.list()!!.toList())
            assertEquals(edited, FileWorkoutRepository(dir).get(workout.id))

            repository.delete(workout.id)

            assertEquals(emptyList<String>(), dir.list()!!.toList())
            assertNull(FileWorkoutRepository(dir).get(workout.id))
        }
}
