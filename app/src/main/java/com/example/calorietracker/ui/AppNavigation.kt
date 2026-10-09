package com.example.calorietracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.ui.screens.AddMealScreen
import com.example.calorietracker.ui.screens.DashboardScreen
import com.example.calorietracker.ui.screens.ProfileScreen
import com.example.calorietracker.viewmodel.AddMealViewModel
import com.example.calorietracker.viewmodel.DashboardViewModel
import com.example.calorietracker.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.first

private const val TRANSITION_MS = 220

private object Routes {
    const val DASHBOARD = "dashboard"
    const val ONBOARDING = "onboarding"
    const val PROFILE = "profile"
    const val MEAL = "meal/{epochDay}?mealId={mealId}"

    fun meal(epochDay: Long, mealId: Long? = null) =
        if (mealId == null) "meal/$epochDay" else "meal/$epochDay?mealId=$mealId"
}

@Composable
fun AppNavigation(repository: CalorieRepository, geminiClient: GeminiClient) {
    // null while we check the database for a saved profile.
    val hasProfile by produceState<Boolean?>(initialValue = null) {
        value = repository.profile.first() != null
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        val profileExists = hasProfile
        if (profileExists == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavGraph(repository, geminiClient, startWithOnboarding = !profileExists)
        }
    }
}

@Composable
private fun NavGraph(
    repository: CalorieRepository,
    geminiClient: GeminiClient,
    startWithOnboarding: Boolean,
) {
    val navController = rememberNavController()
    val startDestination = remember { if (startWithOnboarding) Routes.ONBOARDING else Routes.DASHBOARD }

    // A quick slide + fade (instead of the default slow 700 ms cross-fade).
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(tween(TRANSITION_MS)) { it / 8 } + fadeIn(tween(TRANSITION_MS)) },
        exitTransition = { slideOutHorizontally(tween(TRANSITION_MS)) { -it / 8 } + fadeOut(tween(TRANSITION_MS)) },
        popEnterTransition = { slideInHorizontally(tween(TRANSITION_MS)) { -it / 8 } + fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = { slideOutHorizontally(tween(TRANSITION_MS)) { it / 8 } + fadeOut(tween(TRANSITION_MS)) },
    ) {
        composable(Routes.ONBOARDING) { entry ->
            ProfileScreen(
                viewModel = viewModel { ProfileViewModel(repository, geminiClient) },
                isOnboarding = true,
                onDone = {
                    entry.ifResumed {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) { entry ->
            DashboardScreen(
                viewModel = viewModel { DashboardViewModel(repository) },
                onAddMeal = { day -> entry.ifResumed { navController.navigate(Routes.meal(day.toEpochDay())) } },
                onEditMeal = { meal -> entry.ifResumed { navController.navigate(Routes.meal(meal.epochDay, meal.id)) } },
                onEditProfile = { entry.ifResumed { navController.navigate(Routes.PROFILE) } },
            )
        }
        composable(Routes.PROFILE) { entry ->
            ProfileScreen(
                viewModel = viewModel { ProfileViewModel(repository, geminiClient) },
                isOnboarding = false,
                onDone = { entry.ifResumed { navController.popBackStack() } },
            )
        }
        composable(
            Routes.MEAL,
            arguments = listOf(
                navArgument("epochDay") { type = NavType.LongType },
                navArgument("mealId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val epochDay = entry.arguments?.getLong("epochDay") ?: 0L
            val mealId = entry.arguments?.getLong("mealId")?.takeIf { it >= 0 }
            AddMealScreen(
                viewModel = viewModel { AddMealViewModel(repository, geminiClient, epochDay, mealId) },
                onDone = { entry.ifResumed { navController.popBackStack() } },
            )
        }
    }
}

/**
 * Runs [action] only while this screen is fully shown. Ignores taps that arrive while a
 * transition is running, so a double tap can't pop two screens (leaving a blank screen)
 * or open the same screen twice.
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
