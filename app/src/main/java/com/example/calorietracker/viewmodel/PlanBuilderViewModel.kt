package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.TrainingRepository
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.network.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlanBuilderUiState(
    val goal: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasExistingPlan: Boolean = false,
)

/** "Describe your training goal" → an AI-built weekly plan, saved as the active plan. */
class PlanBuilderViewModel(
    private val training: TrainingRepository,
    private val calories: CalorieRepository,
    private val geminiClient: GeminiClient,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanBuilderUiState())
    val state: StateFlow<PlanBuilderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = training.planInfo.first()
            _state.update { it.copy(hasExistingPlan = existing != null, goal = existing?.prompt.orEmpty()) }
        }
    }

    fun updateGoal(goal: String) = _state.update { it.copy(goal = goal) }

    fun generate(onDone: () -> Unit) {
        val goal = _state.value.goal.trim()
        if (goal.isEmpty() || _state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val context = calories.profileSummary()
            val result = withContext(Dispatchers.IO) { geminiClient.generateWorkoutPlan(goal, context) }
            when (result) {
                is GeminiResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is GeminiResult.Success -> {
                    training.savePlan(result.value, prompt = goal)
                    _state.update { it.copy(isLoading = false) }
                    onDone()
                }
            }
        }
    }
}
