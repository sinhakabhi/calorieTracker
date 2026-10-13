package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.account.Account
import com.example.calorietracker.account.AccountStore
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.DailyTargets
import com.example.calorietracker.data.Profile
import com.example.calorietracker.data.WeightEntry
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.notifications.ReminderSettings
import com.example.calorietracker.notifications.ReminderStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileOverviewState(
    val account: Account? = null,
    val profile: Profile? = null,
    val latestWeight: WeightEntry? = null,
    val targets: DailyTargets? = null,
    val reminders: ReminderSettings = ReminderSettings(),
    val aiRequestsUsed: Int = 0,
)

/** The Profile tab: account, targets, body & goal summary and settings. */
class ProfileOverviewViewModel(
    calories: CalorieRepository,
    accountStore: AccountStore,
    reminderStore: ReminderStore,
    private val geminiClient: GeminiClient,
) : ViewModel() {

    /** Bumped when the screen is shown so the AI usage count is fresh. */
    private val refresh = MutableStateFlow(0)

    val state: StateFlow<ProfileOverviewState> = combine(
        accountStore.account,
        calories.profile,
        calories.weightHistory,
        calories.dailyTargets,
        combine(reminderStore.settings, refresh) { reminders, _ -> reminders to geminiClient.requestsUsedToday() },
    ) { account, profile, weights, targets, (reminders, used) ->
        ProfileOverviewState(account, profile, weights.firstOrNull(), targets, reminders, used)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileOverviewState())

    fun refresh() {
        refresh.value++
    }
}
