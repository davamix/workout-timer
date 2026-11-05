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

/** The completed screen, with everything it shows, so it also comes back after Android closes the app (P7). */
@Serializable
data class CompletedRoute(
    val workoutName: String,
    val totalSeconds: Int,
    val roundCount: Int,
    val exerciseCount: Int,
)
