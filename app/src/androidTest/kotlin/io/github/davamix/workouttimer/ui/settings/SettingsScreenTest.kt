package io.github.davamix.workouttimer.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.settings.InMemorySettingsRepository
import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.ui.assertTouchTargetsAtLeast
import io.github.davamix.workouttimer.ui.theme.WorkoutTimerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The settings screen (P6 SPEC §4.3.7) with an in-memory repository. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var prepareCalls = 0
    private var backs = 0

    private fun show(voiceCuesEnabled: Boolean): InMemorySettingsRepository {
        val repository = InMemorySettingsRepository(Settings(voiceCuesEnabled))
        val viewModel = SettingsViewModel(repository) { prepareCalls++ }
        compose.setContent {
            WorkoutTimerTheme {
                SettingsScreen(onBack = { backs++ }, viewModel = viewModel)
            }
        }
        return repository
    }

    private fun stored(repository: InMemorySettingsRepository) = runBlocking { repository.settings.first().voiceCuesEnabled }

    @Test
    fun theSwitchShowsTheStoredValue() {
        show(voiceCuesEnabled = false)

        compose.onNodeWithTag("voice-cues").assertIsOff()
    }

    @Test
    fun tappingTheSettingStoresItAndPreparesTheAudioWhenTurnedOn() {
        val repository = show(voiceCuesEnabled = true)

        compose.onNodeWithTag("voice-cues").performClick()
        compose.waitUntil { !stored(repository) }
        compose.onNodeWithTag("voice-cues").assertIsOff()
        assertEquals(0, prepareCalls)

        compose.onNodeWithTag("voice-cues").performClick()
        compose.waitUntil { stored(repository) }
        compose.onNodeWithTag("voice-cues").assertIsOn()
        compose.waitUntil { prepareCalls == 1 }
    }

    @Test
    fun theBackArrowCallsBack() {
        show(voiceCuesEnabled = true)

        val back = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.action_back)
        compose.onNodeWithContentDescription(back).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun touchTargetsAreLargeEnough() {
        show(voiceCuesEnabled = true)

        compose.assertTouchTargetsAtLeast()
    }
}
