package com.veyra.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veyra.app.data.CalorieRepository
import com.veyra.app.data.DayCalories
import com.veyra.app.data.WeightEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class ProgressUiState(
    val week: List<DayCalories> = emptyList(),
    val targetKcal: Int? = null,
    /** Newest first. */
    val weights: List<WeightEntry> = emptyList(),
) {
    private val loggedDays get() = week.filter { it.calories > 0 }

    val loggedDayCount: Int get() = loggedDays.size

    /** Average intake over the days that have meals logged, or null if none. */
    val averageKcal: Int? get() = loggedDays.takeIf { it.isNotEmpty() }?.map { it.calories }?.average()?.roundToInt()

    /** Logged days that stayed within the calorie target. */
    val daysWithinTarget: Int get() = targetKcal?.let { t -> loggedDays.count { it.calories <= t } } ?: 0

    val latestWeight: WeightEntry? get() = weights.firstOrNull()

    /** Change from the oldest to the latest reading, or null with fewer than two readings. */
    val weightChangeKg: Double? get() = if (weights.size < 2) null else weights.first().weightKg - weights.last().weightKg
}

class ProgressViewModel(private val repository: CalorieRepository) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.lastSevenDays(LocalDate.now()),
        repository.dailyTargets,
        repository.weightHistory,
    ) { week, targets, weights ->
        ProgressUiState(week = week, targetKcal = targets?.kcal, weights = weights)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun logWeight(weightKg: Double) {
        viewModelScope.launch { repository.logWeight(LocalDate.now(), weightKg) }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch { repository.deleteWeight(entry) }
    }
}
