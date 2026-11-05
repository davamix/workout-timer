package io.github.davamix.workouttimer.ui.settings

import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.ui.MainDispatcherExtension
import io.github.davamix.workouttimer.ui.TestSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SettingsViewModelTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private var prepareCalls = 0

    private fun viewModel(repository: TestSettingsRepository) = SettingsViewModel(repository) { prepareCalls++ }

    private suspend fun TestSettingsRepository.voiceCuesEnabled() = settings.first().voiceCuesEnabled

    @Test
    fun `it shows the stored value`() =
        runTest {
            val viewModel = viewModel(TestSettingsRepository(Settings(voiceCuesEnabled = false)))

            assertEquals(SettingsState(voiceCuesEnabled = false), viewModel.state.value)
        }

    @Test
    fun `turning voice cues off, then on, stores each value`() =
        runTest {
            val repository = TestSettingsRepository()
            val viewModel = viewModel(repository)

            viewModel.setVoiceCuesEnabled(false)
            assertFalse(repository.voiceCuesEnabled())
            assertEquals(false, viewModel.state.value.voiceCuesEnabled)

            viewModel.setVoiceCuesEnabled(true)
            assertTrue(repository.voiceCuesEnabled())
            assertEquals(true, viewModel.state.value.voiceCuesEnabled)
        }

    @Test
    fun `turning voice cues on prepares the audio, and turning them off does not`() =
        runTest {
            val viewModel = viewModel(TestSettingsRepository(Settings(voiceCuesEnabled = false)))

            viewModel.setVoiceCuesEnabled(true)
            assertEquals(1, prepareCalls)

            viewModel.setVoiceCuesEnabled(false)
            assertEquals(1, prepareCalls)
        }

    @Test
    fun `a failed write says so, keeps the stored value and prepares nothing`() =
        runTest {
            val repository = TestSettingsRepository(Settings(voiceCuesEnabled = false)).apply { failWrites = true }
            val viewModel = viewModel(repository)

            viewModel.setVoiceCuesEnabled(true)

            assertEquals(SettingsState(voiceCuesEnabled = false, saveFailed = true), viewModel.state.value)
            assertFalse(repository.voiceCuesEnabled())
            assertEquals(0, prepareCalls)

            viewModel.messageShown()
            assertFalse(viewModel.state.value.saveFailed)
        }
}
