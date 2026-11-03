package io.github.davamix.workouttimer.data

import io.github.davamix.workouttimer.core.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSettingsRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val file by lazy { File(dir, "settings.preferences_pb") }

    private val warnings = mutableListOf<Pair<String, Throwable>>()

    /** A DataStore scope of its own, so a test can cancel it to simulate the app stopping. */
    private fun TestScope.dataStoreScope() = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

    private fun repository(scope: CoroutineScope) =
        DataStoreSettingsRepository(createSettingsDataStore(file, scope) { message, error -> warnings += message to error })

    @Test
    fun `with no file, the settings are the defaults`() =
        runTest {
            val repository = repository(backgroundScope)

            assertEquals(Settings(), repository.settings.first())
            assertTrue(repository.settings.first().voiceCuesEnabled)
        }

    @Test
    fun `voice cues can be turned off and on again`() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setVoiceCuesEnabled(false)
            assertFalse(repository.settings.first().voiceCuesEnabled)

            repository.setVoiceCuesEnabled(true)
            assertTrue(repository.settings.first().voiceCuesEnabled)
        }

    @Test
    fun `the setting is remembered by a new DataStore on the same file`() =
        runTest {
            val firstScope = dataStoreScope()
            repository(firstScope).setVoiceCuesEnabled(false)
            // The app stops: DataStore only lets go of the file once its scope is done.
            firstScope.coroutineContext.job.cancelAndJoin()

            val afterRestart = repository(backgroundScope)

            assertFalse(afterRestart.settings.first().voiceCuesEnabled)
            assertTrue(file.isFile)
        }

    @Test
    fun `a corrupted file gives the defaults and is reported`() =
        runTest {
            // 'g' starts a protobuf field with wire type 7, which doesn't exist, so the file can't be parsed.
            file.writeText("garbage, not a protobuf")
            val repository = repository(backgroundScope)

            assertEquals(Settings(), repository.settings.first())
            assertTrue(warnings.single().first.contains("settings.preferences_pb"))

            repository.setVoiceCuesEnabled(false)
            assertFalse(repository.settings.first().voiceCuesEnabled)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `settings emits each change, and not when a value is set again`() =
        runTest {
            val repository = repository(backgroundScope)
            val emissions = mutableListOf<Settings>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.settings.toList(emissions) }

            repository.setVoiceCuesEnabled(false)
            repository.setVoiceCuesEnabled(false)
            repository.setVoiceCuesEnabled(true)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(true, false, true), emissions.map { it.voiceCuesEnabled })
        }
}
