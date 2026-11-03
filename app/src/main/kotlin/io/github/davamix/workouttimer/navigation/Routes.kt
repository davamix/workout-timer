package io.github.davamix.workouttimer.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WorkoutListRoute

/** [workoutId] is null when creating a new workout. */
@Serializable
data class WorkoutEditorRoute(
    val workoutId: String? = null,
)

@Serializable
data object SettingsRoute

@Serializable
data class RunnerRoute(
    val workoutId: String,
)

@Serializable
data object CompletedRoute
