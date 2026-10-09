package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.Profile
import com.example.calorietracker.data.WeightEntry
import com.example.calorietracker.domain.ActivityLevel
import com.example.calorietracker.domain.CalorieCalculator
import com.example.calorietracker.domain.Goal
import com.example.calorietracker.domain.MacroTargets
import com.example.calorietracker.domain.Sex
import com.example.calorietracker.ui.formatNumber
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.network.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** The profile form. Numbers are kept as text so the fields can be edited freely. */
data class ProfileForm(
    val age: String = "",
    val sex: Sex = Sex.MALE,
    val heightCm: String = "",
    val weightKg: String = "",
    val activity: ActivityLevel = ActivityLevel.SEDENTARY,
    val goal: Goal = Goal.MAINTAIN,
) {
    val ageValue: Int? get() = age.trim().toIntOrNull()
    val heightValue: Double? get() = heightCm.trim().toDoubleOrNull()
    val weightValue: Double? get() = weightKg.trim().toDoubleOrNull()

    val ageValid: Boolean get() = CalorieCalculator.isValidAge(ageValue)
    val heightValid: Boolean get() = CalorieCalculator.isValidHeight(heightValue)
    val weightValid: Boolean get() = CalorieCalculator.isValidWeight(weightValue)
    val isValid: Boolean get() = ageValid && heightValid && weightValid

    /** Calories burned at complete rest, or null while the form is invalid. */
    val previewBmr: Int?
        get() = if (!isValid) null else CalorieCalculator.bmr(ageValue!!, sex, heightValue!!, weightValue!!).roundToInt()

    /** Daily macro targets, or null while the form is invalid. */
    val previewMacros: MacroTargets?
        get() = previewTarget?.let { CalorieCalculator.macroTargets(it, weightValue!!, activity) }

    /** Live preview of the daily target, or null while the form is invalid. */
    val previewTarget: Int?
        get() = if (!isValid) null else CalorieCalculator.dailyTarget(
            age = ageValue!!,
            sex = sex,
            heightCm = heightValue!!,
            weightKg = weightValue!!,
            activity = activity,
            goal = goal,
        )
}

/** State of the "describe yourself" AI assist card. */
data class ProfileAssistState(
    val text: String = "",
    val isLoading: Boolean = false,
    /** Shown after a successful fill, e.g. what Gemini inferred. */
    val message: String? = null,
    val error: String? = null,
)

class ProfileViewModel(
    private val repository: CalorieRepository,
    private val geminiClient: GeminiClient,
) : ViewModel() {

    private val _form = MutableStateFlow(ProfileForm())
    val form: StateFlow<ProfileForm> = _form.asStateFlow()

    private val _assist = MutableStateFlow(ProfileAssistState())
    val assist: StateFlow<ProfileAssistState> = _assist.asStateFlow()

    val weightHistory: StateFlow<List<WeightEntry>> = repository.weightHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Pre-fill the form with the saved profile and latest weight, if any.
        viewModelScope.launch {
            val profile = repository.profile.first() ?: return@launch
            val latestWeight = repository.weightHistory.first().firstOrNull()
            _form.value = ProfileForm(
                age = profile.age.toString(),
                sex = profile.sex,
                heightCm = formatNumber(profile.heightCm),
                weightKg = latestWeight?.let { formatNumber(it.weightKg) }.orEmpty(),
                activity = profile.activityLevel,
                goal = profile.goal,
            )
        }
    }

    fun update(transform: (ProfileForm) -> ProfileForm) = _form.update(transform)

    fun updateAssistText(text: String) = _assist.update { it.copy(text = text) }

    /** Asks Gemini to read the description and fills in only the fields it found. */
    fun fillFromDescription() {
        val description = _assist.value.text.trim()
        if (description.isEmpty() || _assist.value.isLoading) return
        _assist.update { it.copy(isLoading = true, message = null, error = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { geminiClient.extractProfile(description) }
            when (result) {
                is GeminiResult.Error -> _assist.update { it.copy(isLoading = false, error = result.message) }
                is GeminiResult.Success -> {
                    val found = result.value
                    if (found.isEmpty) {
                        _assist.update {
                            it.copy(
                                isLoading = false,
                                error = "Couldn't find any profile details in that. Try mentioning your age, height and weight.",
                            )
                        }
                        return@launch
                    }
                    _form.update { form ->
                        form.copy(
                            age = found.age?.toString() ?: form.age,
                            sex = found.sex ?: form.sex,
                            heightCm = found.heightCm?.let { formatNumber(roundToTenth(it)) } ?: form.heightCm,
                            weightKg = found.weightKg?.let { formatNumber(roundToTenth(it)) } ?: form.weightKg,
                            activity = found.activity ?: form.activity,
                            goal = found.goal ?: form.goal,
                        )
                    }
                    val summary = found.note.ifBlank { "Filled in what I could find." }
                    _assist.update {
                        it.copy(isLoading = false, message = "$summary Check the fields below before saving.")
                    }
                }
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val form = _form.value
        if (!form.isValid) return
        viewModelScope.launch {
            repository.saveProfile(
                Profile(
                    age = form.ageValue!!,
                    sex = form.sex,
                    heightCm = form.heightValue!!,
                    activityLevel = form.activity,
                    goal = form.goal,
                ),
                weightKg = form.weightValue!!,
            )
            onSaved()
        }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch { repository.deleteWeight(entry) }
    }
}

/** Converted units (e.g. 5'11" → 180.34 cm) are rounded to one decimal place for the form. */
private fun roundToTenth(value: Double): Double = (value * 10).roundToInt() / 10.0
