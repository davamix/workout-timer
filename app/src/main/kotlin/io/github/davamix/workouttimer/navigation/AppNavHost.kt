package io.github.davamix.workouttimer.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.davamix.workouttimer.ui.completed.CompletedScreen
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
        composable<WorkoutListRoute> {
            WorkoutListScreen(
                onNewWorkout = dropUnlessResumed { navController.navigate(WorkoutEditorRoute()) },
                onOpenSettings = dropUnlessResumed { navController.navigate(SettingsRoute) },
                onStartWorkout = { workoutId -> navController.navigate(RunnerRoute(workoutId)) },
            )
        }
        composable<WorkoutEditorRoute> { entry ->
            WorkoutEditorScreen(
                workoutId = entry.toRoute<WorkoutEditorRoute>().workoutId,
                onBack = dropUnlessResumed { navController.popBackStack() },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<RunnerRoute> { entry ->
            RunnerScreen(
                workoutId = entry.toRoute<RunnerRoute>().workoutId,
                onFinished =
                    dropUnlessResumed {
                        // The runner leaves the back stack, so Back on the completed screen returns to the list.
                        navController.navigate(CompletedRoute) {
                            popUpTo<RunnerRoute> { inclusive = true }
                        }
                    },
            )
        }
        composable<CompletedRoute> {
            CompletedScreen(
                onBackToList = dropUnlessResumed { navController.popBackStack(WorkoutListRoute, inclusive = false) },
            )
        }
    }
}
