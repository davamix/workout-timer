package io.github.davamix.workouttimer.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.davamix.workouttimer.ui.completed.CompletedScreen
import io.github.davamix.workouttimer.ui.completed.WorkoutSummary
import io.github.davamix.workouttimer.ui.editor.WorkoutEditorScreen
import io.github.davamix.workouttimer.ui.list.WorkoutListScreen
import io.github.davamix.workouttimer.ui.runner.RunnerScreen
import io.github.davamix.workouttimer.ui.settings.SettingsScreen

/**
 * The app's navigation graph. Every screen has its destination here from P1 on; later phases replace the screen
 * bodies and only adjust the arguments they need.
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = WorkoutListRoute) {
        composable<WorkoutListRoute> { entry ->
            WorkoutListScreen(
                onNewWorkout = dropUnlessResumed { navController.navigate(WorkoutEditorRoute()) },
                onEditWorkout = { workoutId -> entry.ifResumed { navController.navigate(WorkoutEditorRoute(workoutId)) } },
                onStartWorkout = { workoutId -> entry.ifResumed { navController.navigate(RunnerRoute(workoutId)) } },
                onOpenSettings = dropUnlessResumed { navController.navigate(SettingsRoute) },
            )
        }
        composable<WorkoutEditorRoute> { entry ->
            WorkoutEditorScreen(
                workoutId = entry.toRoute<WorkoutEditorRoute>().workoutId,
                onClose = dropUnlessResumed { navController.popBackStack() },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<RunnerRoute> { entry ->
            // The runner calls these once each, after waiting until it's resumed (P7 SPEC §4.3.7). dropUnlessResumed
            // would drop the call of a runner rebuilt after Android closed the app, which is composed before then.
            RunnerScreen(
                workoutId = entry.toRoute<RunnerRoute>().workoutId,
                onCompleted = { summary ->
                    // The runner leaves the back stack, so Back on the completed screen returns to the list.
                    navController.navigate(summary.toRoute()) {
                        popUpTo<RunnerRoute> { inclusive = true }
                    }
                },
                onExit = { navController.popBackStack(WorkoutListRoute, inclusive = false) },
            )
        }
        composable<CompletedRoute> { entry ->
            CompletedScreen(
                summary = entry.toRoute<CompletedRoute>().toSummary(),
                onBackToList = dropUnlessResumed { navController.popBackStack(WorkoutListRoute, inclusive = false) },
            )
        }
    }
}

/**
 * Runs [action] only while this destination is resumed, as `dropUnlessResumed` does for callbacks without an
 * argument. A double tap then can't open a screen twice.
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) action()
}

private fun WorkoutSummary.toRoute() = CompletedRoute(workoutName, totalSeconds, roundCount, exerciseCount)

private fun CompletedRoute.toSummary() = WorkoutSummary(workoutName, totalSeconds, roundCount, exerciseCount)
