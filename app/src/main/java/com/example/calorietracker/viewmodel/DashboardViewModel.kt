package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.DayCalories
import com.example.calorietracker.data.Meal
import com.example.calorietracker.domain.MacroTargets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val date: LocalDate = LocalDate.now(),
    val targetKcal: Int? = null,
    val macroTargets: MacroTargets? = null,
    val waterTargetMl: Int? = null,
    val waterMl: Int = 0,
    val meals: List<Meal> = emptyList(),
    val week: List<DayCalories> = emptyList(),
) {
    val eatenKcal: Int get() = meals.sumOf { it.calories }
    val proteinG: Double get() = meals.sumOf { it.proteinG }
    val carbsG: Double get() = meals.sumOf { it.carbsG }
    val fatG: Double get() = meals.sumOf { it.fatG }
    val isToday: Boolean get() = date == LocalDate.now()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: CalorieRepository) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DashboardUiState> = selectedDate
        .flatMapLatest { date ->
            combine(
                repository.dailyTargets,
                repository.mealsForDay(date),
                repository.lastSevenDays(date),
                repository.waterForDay(date),
            ) { targets, meals, week, waterMl ->
                DashboardUiState(
                    date = date,
                    targetKcal = targets?.kcal,
                    macroTargets = targets?.macros,
                    waterTargetMl = targets?.waterMl,
                    waterMl = waterMl,
                    meals = meals,
                    week = week,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun previousDay() = selectedDate.update { it.minusDays(1) }

    fun nextDay() = selectedDate.update { date ->
        if (date < LocalDate.now()) date.plusDays(1) else date
    }

    /** Adds water to the day being viewed; a negative amount removes some. */
    fun addWater(deltaMl: Int) {
        val date = selectedDate.value
        viewModelScope.launch { repository.addWater(date, deltaMl) }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch { repository.deleteMeal(meal) }
    }

    /** Puts a deleted meal back (same id), used by the snackbar's Undo. */
    fun restoreMeal(meal: Meal) {
        viewModelScope.launch { repository.saveMeal(meal) }
    }
}
