package com.example.calorietracker.ui

import androidx.compose.animation.AnimatedContentTransitionScope
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calorietracker.CalorieTrackerApp
import com.example.calorietracker.notifications.Reminders
import com.example.calorietracker.ui.components.MainTab
import com.example.calorietracker.ui.screens.AddMealScreen
import com.example.calorietracker.ui.screens.DashboardScreen
import com.example.calorietracker.ui.screens.LoginScreen
import com.example.calorietracker.ui.screens.NotificationsScreen
import com.example.calorietracker.ui.screens.PlanBuilderScreen
import com.example.calorietracker.ui.screens.ProfileEditScreen
import com.example.calorietracker.ui.screens.ProfileScreen
import com.example.calorietracker.ui.screens.ProgressScreen
import com.example.calorietracker.ui.screens.TodayScreen
import com.example.calorietracker.ui.screens.TrainingScreen
import com.example.calorietracker.viewmodel.AddMealViewModel
import com.example.calorietracker.viewmodel.DashboardViewModel
import com.example.calorietracker.viewmodel.PlanBuilderViewModel
import com.example.calorietracker.viewmodel.ProfileOverviewViewModel
import com.example.calorietracker.viewmodel.ProfileViewModel
import com.example.calorietracker.viewmodel.ProgressViewModel
import com.example.calorietracker.viewmodel.TodayViewModel
import com.example.calorietracker.viewmodel.TrainingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TRANSITION_MS = 220

/** Switching between bottom tabs is a quick cross-fade rather than a slide. */
private const val TAB_FADE_MS = 150

private object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"

    // Bottom tabs
    const val TODAY = "today"
    const val NUTRITION = "nutrition"
    const val TRAINING = "training"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    // Pushed screens
    const val PROFILE_EDIT = "profile/edit"
    const val NOTIFICATIONS = "notifications"
    const val MEAL = "meal/{epochDay}?mealId={mealId}"
    const val PLAN_BUILDER = "plan/new"

    val TABS = setOf(TODAY, NUTRITION, TRAINING, PROGRESS, PROFILE)

    fun tab(tab: MainTab) = when (tab) {
        MainTab.TODAY -> TODAY
        MainTab.NUTRITION -> NUTRITION
        MainTab.TRAINING -> TRAINING
        MainTab.PROGRESS -> PROGRESS
        MainTab.PROFILE -> PROFILE
    }

    fun meal(epochDay: Long, mealId: Long? = null) =
        if (mealId == null) "meal/$epochDay" else "meal/$epochDay?mealId=$mealId"
}

@Composable
fun AppNavigation(app: CalorieTrackerApp) {
    // Where to start: null while the database is checked for a saved profile.
    val start by produceState<String?>(initialValue = null) {
        value = when {
            app.accountStore.account.value == null -> Routes.LOGIN
            app.repository.profile.first() == null -> Routes.ONBOARDING
            else -> Routes.TODAY
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        val startDestination = start
        if (startDestination == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            NavGraph(app, startDestination)
        }
    }
}

@Composable
private fun NavGraph(app: CalorieTrackerApp, startRoute: String) {
    val navController = rememberNavController()
    val startDestination = remember { startRoute }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = app.repository
    val trainingRepository = app.trainingRepository
    val geminiClient = app.geminiClient

    // Bottom tabs: keep each tab's state, and don't stack tabs on top of each other.
    fun switchTab(tab: MainTab) = navController.navigate(Routes.tab(tab)) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    /** After sign-in: set up the profile first if there isn't one yet. */
    fun afterSignIn() = scope.launch {
        val next = if (repository.profile.first() == null) Routes.ONBOARDING else Routes.TODAY
        navController.navigateClearingStack(next)
    }

    // A quick slide + fade (instead of the default slow 700 ms cross-fade); tab switches just fade.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (isTabSwitch()) fadeIn(tween(TAB_FADE_MS))
            else slideInHorizontally(tween(TRANSITION_MS)) { it / 8 } + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = {
            if (isTabSwitch()) fadeOut(tween(TAB_FADE_MS))
            else slideOutHorizontally(tween(TRANSITION_MS)) { -it / 8 } + fadeOut(tween(TRANSITION_MS))
        },
        popEnterTransition = {
            if (isTabSwitch()) fadeIn(tween(TAB_FADE_MS))
            else slideInHorizontally(tween(TRANSITION_MS)) { -it / 8 } + fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = {
            if (isTabSwitch()) fadeOut(tween(TAB_FADE_MS))
            else slideOutHorizontally(tween(TRANSITION_MS)) { it / 8 } + fadeOut(tween(TRANSITION_MS))
        },
    ) {
        composable(Routes.LOGIN) { entry ->
            LoginScreen(
                googleAuth = app.googleAuth,
                onSignedIn = { name, email ->
                    app.accountStore.signIn(name, email)
                    entry.ifResumed { afterSignIn() }
                },
                onContinueAsGuest = {
                    app.accountStore.signIn(name = "", email = null)
                    entry.ifResumed { afterSignIn() }
                },
            )
        }
        composable(Routes.ONBOARDING) { entry ->
            ProfileEditScreen(
                viewModel = viewModel { ProfileViewModel(repository, geminiClient) },
                isOnboarding = true,
                onDone = { entry.ifResumed { navController.navigateClearingStack(Routes.TODAY) } },
            )
        }

        // ---- Tabs ----
        composable(Routes.TODAY) { entry ->
            TodayScreen(
                viewModel = viewModel { TodayViewModel(repository, trainingRepository, app.accountStore) },
                onSelectTab = { tab -> entry.ifResumed { switchTab(tab) } },
                onLogFood = { entry.ifResumed { navController.navigate(Routes.meal(LocalDate.now().toEpochDay())) } },
                onCreatePlan = { entry.ifResumed { navController.navigate(Routes.PLAN_BUILDER) } },
                onOpenNotifications = { entry.ifResumed { navController.navigate(Routes.NOTIFICATIONS) } },
            )
        }
        composable(Routes.NUTRITION) { entry ->
            DashboardScreen(
                viewModel = viewModel { DashboardViewModel(repository) },
                onAddMeal = { day -> entry.ifResumed { navController.navigate(Routes.meal(day.toEpochDay())) } },
                onEditMeal = { meal -> entry.ifResumed { navController.navigate(Routes.meal(meal.epochDay, meal.id)) } },
                onSelectTab = { tab -> entry.ifResumed { switchTab(tab) } },
            )
        }
        composable(Routes.TRAINING) { entry ->
            TrainingScreen(
                viewModel = viewModel { TrainingViewModel(trainingRepository, repository, geminiClient) },
                onSelectTab = { tab -> entry.ifResumed { switchTab(tab) } },
                onCreateWithAi = { entry.ifResumed { navController.navigate(Routes.PLAN_BUILDER) } },
            )
        }
        composable(Routes.PROGRESS) { entry ->
            ProgressScreen(
                viewModel = viewModel { ProgressViewModel(repository) },
                onSelectTab = { tab -> entry.ifResumed { switchTab(tab) } },
            )
        }
        composable(Routes.PROFILE) { entry ->
            ProfileScreen(
                viewModel = viewModel { ProfileOverviewViewModel(repository, app.accountStore, app.reminderStore, geminiClient) },
                onSelectTab = { tab -> entry.ifResumed { switchTab(tab) } },
                onEditProfile = { entry.ifResumed { navController.navigate(Routes.PROFILE_EDIT) } },
                onOpenNotifications = { entry.ifResumed { navController.navigate(Routes.NOTIFICATIONS) } },
                onSignIn = if (!app.googleAuth.isConfigured) null else {
                    {
                        // A guest signs in: drop the guest session and show the sign-in screen.
                        app.accountStore.signOut()
                        navController.navigateClearingStack(Routes.LOGIN)
                    }
                },
                onSignOut = {
                    scope.launch {
                        app.googleAuth.signOut(context)
                        app.accountStore.signOut()
                        navController.navigateClearingStack(Routes.LOGIN)
                    }
                },
            )
        }

        // ---- Pushed screens ----
        composable(Routes.PROFILE_EDIT) { entry ->
            ProfileEditScreen(
                viewModel = viewModel { ProfileViewModel(repository, geminiClient) },
                isOnboarding = false,
                onDone = { entry.ifResumed { navController.popBackStack() } },
            )
        }
        composable(Routes.NOTIFICATIONS) { entry ->
            val settings by app.reminderStore.settings.collectAsStateWithLifecycle()
            NotificationsScreen(
                settings = settings,
                onChange = { newSettings ->
                    app.reminderStore.update(newSettings)
                    Reminders.schedule(context, newSettings)
                },
                onBack = { entry.ifResumed { navController.popBackStack() } },
            )
        }
        composable(Routes.PLAN_BUILDER) { entry ->
            PlanBuilderScreen(
                viewModel = viewModel { PlanBuilderViewModel(trainingRepository, repository, geminiClient) },
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

/** Goes to [route] and removes everything behind it (sign-in, sign-out, finishing setup). */
private fun NavController.navigateClearingStack(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in Routes.TABS && targetState.destination.route in Routes.TABS

/**
 * Runs [action] only while this screen is fully shown. Ignores taps that arrive while a
 * transition is running, so a double tap can't pop two screens (leaving a blank screen)
 * or open the same screen twice.
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
