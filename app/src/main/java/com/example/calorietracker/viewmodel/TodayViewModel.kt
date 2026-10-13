package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.account.Account
import com.example.calorietracker.account.AccountStore
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.DailyTargets
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.PlanDay
import com.example.calorietracker.data.TrainingRepository
import com.example.calorietracker.data.WeightEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Today's workout, as shown on the Today hub. */
data class TodayWorkout(
    val hasPlan: Boolean,
    val day: PlanDay?,
    val total: Int,
    val done: Int,
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val targets: DailyTargets? = null,
    val meals: List<Meal> = emptyList(),
    val waterMl: Int = 0,
    val workout: TodayWorkout = TodayWorkout(hasPlan = false, day = null, total = 0, done = 0),
    val latestWeight: WeightEntry? = null,
) {
    val eatenKcal: Int get() = meals.sumOf { it.calories }
    val proteinG: Double get() = meals.sumOf { it.proteinG }
    val carbsG: Double get() = meals.sumOf { it.carbsG }
    val fatG: Double get() = meals.sumOf { it.fatG }
}

/** The Today hub: a summary of nutrition, training, water and weight for today. */
class TodayViewModel(
    private val calories: CalorieRepository,
    training: TrainingRepository,
    accountStore: AccountStore,
) : ViewModel() {

    private val today = LocalDate.now()

    private val workout = combine(
        training.planInfo,
        training.days,
        training.exercises,
        training.completions(today, today),
    ) { plan, days, exercises, completions ->
        val todays = exercises.filter { it.dayOfWeek == today.dayOfWeek.value }
        val done = completions.map { it.exerciseId }.toSet()
        TodayWorkout(
            hasPlan = plan != null,
            day = days.firstOrNull { it.dayOfWeek == today.dayOfWeek.value },
            total = todays.size,
            done = todays.count { it.id in done },
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(
        accountStore.account,
        calories.dailyTargets,
        calories.mealsForDay(today),
        calories.waterForDay(today),
        combine(workout, calories.weightHistory) { w, weights -> w to weights.firstOrNull() },
    ) { account, targets, meals, water, (workout, weight) ->
        TodayUiState(
            isLoading = false,
            account = account,
            targets = targets,
            meals = meals,
            waterMl = water,
            workout = workout,
            latestWeight = weight,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun addWater(deltaMl: Int) {
        viewModelScope.launch { calories.addWater(today, deltaMl) }
    }
}
