package io.github.davamix.workouttimer.core.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InMemorySettingsRepositoryTest {
    @Test
    fun `voice cues are on by default`() =
        runTest {
            assertTrue(Settings().voiceCuesEnabled)
            assertTrue(
                InMemorySettingsRepository()
                    .settings
                    .first()
                    .voiceCuesEnabled,
            )
        }

    @Test
    fun `a repository starts with its initial settings`() =
        runTest {
            val repository = InMemorySettingsRepository(Settings(voiceCuesEnabled = false))

            assertFalse(repository.settings.first().voiceCuesEnabled)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `settings emits each change, and not when a value is set again`() =
        runTest {
            val repository = InMemorySettingsRepository()
            val emissions = mutableListOf<Settings>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.settings.toList(emissions) }

            repository.setVoiceCuesEnabled(false)
            repository.setVoiceCuesEnabled(false)
            repository.setVoiceCuesEnabled(true)

            assertEquals(listOf(true, false, true), emissions.map { it.voiceCuesEnabled })
        }
}
