package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.MealType
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.network.GeminiResult
import com.example.calorietracker.ui.formatNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

data class AddMealUiState(
    val description: String = "",
    val name: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val note: String = "",
    val mealType: MealType = defaultMealType(),
    val isEditing: Boolean = false,
    val isEstimating: Boolean = false,
    val error: String? = null,
) {
    val caloriesValue: Int? get() = calories.trim().toIntOrNull()?.takeIf { it >= 0 }

    /** Macros are optional; a blank field counts as 0, anything else must be a non-negative number. */
    private fun macroValue(text: String): Double? =
        if (text.isBlank()) 0.0 else text.trim().toDoubleOrNull()?.takeIf { it >= 0 }

    val proteinValue get() = macroValue(protein)
    val carbsValue get() = macroValue(carbs)
    val fatValue get() = macroValue(fat)

    val canSave: Boolean
        get() = name.isNotBlank() && caloriesValue != null &&
            proteinValue != null && carbsValue != null && fatValue != null && !isEstimating
}

/** Breakfast before 11:00, lunch until 16:00, snack until 19:00, dinner otherwise. */
fun defaultMealType(time: LocalTime = LocalTime.now()): MealType = when (time.hour) {
    in 4..10 -> MealType.BREAKFAST
    in 11..15 -> MealType.LUNCH
    in 16..18 -> MealType.SNACK
    else -> MealType.DINNER
}

/**
 * Add a new meal for [epochDay], or edit an existing one when [mealId] is not null.
 */
class AddMealViewModel(
    private val repository: CalorieRepository,
    private val geminiClient: GeminiClient,
    private val epochDay: Long,
    private val mealId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddMealUiState(
            isEditing = mealId != null,
            error = if (geminiClient.hasApiKey) null else GeminiClient.MISSING_KEY_MESSAGE,
        )
    )
    val state: StateFlow<AddMealUiState> = _state.asStateFlow()

    private var existingMeal: Meal? = null

    init {
        if (mealId != null) {
            viewModelScope.launch {
                val meal = repository.getMeal(mealId) ?: return@launch
                existingMeal = meal
                _state.update {
                    it.copy(
                        name = meal.name,
                        calories = meal.calories.toString(),
                        protein = formatNumber(meal.proteinG),
                        carbs = formatNumber(meal.carbsG),
                        fat = formatNumber(meal.fatG),
                        note = meal.note,
                        mealType = meal.mealType,
                    )
                }
            }
        }
    }

    fun update(transform: (AddMealUiState) -> AddMealUiState) = _state.update(transform)

    fun estimate() {
        val description = _state.value.description.trim()
        if (description.isEmpty() || _state.value.isEstimating) return
        _state.update { it.copy(isEstimating = true, error = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { geminiClient.estimateMeal(description) }
            _state.update {
                when (result) {
                    is GeminiResult.Success -> it.copy(
                        isEstimating = false,
                        name = result.value.name,
                        calories = result.value.calories.toString(),
                        protein = formatNumber(result.value.proteinG),
                        carbs = formatNumber(result.value.carbsG),
                        fat = formatNumber(result.value.fatG),
                        note = result.value.note,
                    )
                    is GeminiResult.Error -> it.copy(isEstimating = false, error = result.message)
                }
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        val existing = existingMeal
        val meal = Meal(
            id = existing?.id ?: 0,
            epochDay = existing?.epochDay ?: epochDay,
            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
            name = s.name.trim(),
            calories = s.caloriesValue!!,
            proteinG = s.proteinValue!!,
            carbsG = s.carbsValue!!,
            fatG = s.fatValue!!,
            mealType = s.mealType,
            note = s.note.trim(),
        )
        viewModelScope.launch {
            repository.saveMeal(meal)
            onSaved()
        }
    }
}
