package io.github.davamix.workouttimer.core.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A [SettingsRepository] kept in memory, for tests and previews. Nothing survives the process. It is safe to use from
 * several coroutines at once.
 *
 * @param initial The settings it starts with.
 */
class InMemorySettingsRepository(
    initial: Settings = Settings(),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<Settings> = state

    override suspend fun setVoiceCuesEnabled(enabled: Boolean) {
        state.update { it.copy(voiceCuesEnabled = enabled) }
    }
}
