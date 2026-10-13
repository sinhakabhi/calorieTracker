package com.example.calorietracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.DaySnapshot
import com.example.calorietracker.data.PlanDay
import com.example.calorietracker.data.PlanExercise
import com.example.calorietracker.data.PlanInfo
import com.example.calorietracker.data.TrainingRepository
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.network.GeminiResult
import com.example.calorietracker.ui.formatPrescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TrainingUiState(
    val isLoading: Boolean = true,
    val plan: PlanInfo? = null,
    /** Monday … Sunday of the current week. */
    val week: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val days: Map<Int, PlanDay> = emptyMap(),
    val exercisesByDay: Map<Int, List<PlanExercise>> = emptyMap(),
    /** (epochDay, exerciseId) pairs marked done this week. */
    val completions: Set<Pair<Long, Long>> = emptySet(),
) {
    val today: LocalDate get() = LocalDate.now()

    fun dayFor(date: LocalDate): PlanDay? = days[date.dayOfWeek.value]
    fun exercisesFor(date: LocalDate): List<PlanExercise> = exercisesByDay[date.dayOfWeek.value].orEmpty()
    fun isDone(date: LocalDate, exercise: PlanExercise) = (date.toEpochDay() to exercise.id) in completions
    fun doneCount(date: LocalDate) = exercisesFor(date).count { isDone(date, it) }

    /** Today and earlier days can be checked off; future days can't. */
    fun canCheck(date: LocalDate) = !date.isAfter(today)
}

/** State of an AI request for one day: the "Edit day with AI" dialog or a one-tap swap. */
data class DayAiState(
    val dayOfWeek: Int,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** A short message for the snackbar, optionally with Undo. */
data class TrainingMessage(val text: String, val undo: DaySnapshot? = null)

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val calories: CalorieRepository,
    private val geminiClient: GeminiClient,
) : ViewModel() {

    private val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val week = (0L..6L).map { monday.plusDays(it) }
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TrainingUiState> = combine(
        repository.planInfo,
        repository.days,
        repository.exercises,
        repository.completions(week.first(), week.last()),
        selectedDate,
    ) { plan, days, exercises, completions, selected ->
        TrainingUiState(
            isLoading = false,
            plan = plan,
            week = week,
            selectedDate = selected,
            days = days.associateBy { it.dayOfWeek },
            exercisesByDay = exercises.groupBy { it.dayOfWeek },
            completions = completions.map { it.epochDay to it.exerciseId }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingUiState(week = week))

    private val _dayAi = MutableStateFlow<DayAiState?>(null)
    val dayAi: StateFlow<DayAiState?> = _dayAi.asStateFlow()

    /** Id of the exercise currently being swapped by AI, for a progress hint on its row. */
    val swapping = MutableStateFlow<Long?>(null)

    private val _messages = Channel<TrainingMessage>(Channel.BUFFERED)
    val messages: Flow<TrainingMessage> = _messages.receiveAsFlow()

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun toggleDone(date: LocalDate, exercise: PlanExercise) {
        val state = uiState.value
        if (!state.canCheck(date)) return
        val done = !state.isDone(date, exercise)
        viewModelScope.launch { repository.setCompleted(date, exercise.id, done) }
    }

    // ---- Manual editing ----

    fun saveExercise(exercise: PlanExercise) = launch { repository.saveExercise(exercise) }
    fun deleteExercise(exercise: PlanExercise) = launch {
        val before = repository.snapshot(exercise.dayOfWeek)
        repository.deleteExercise(exercise.id)
        _messages.send(TrainingMessage("Deleted ${exercise.name}", undo = before))
    }
    fun moveUp(exercise: PlanExercise) = launch { repository.moveExercise(exercise, -1) }
    fun moveDown(exercise: PlanExercise) = launch { repository.moveExercise(exercise, +1) }
    fun duplicate(exercise: PlanExercise) = launch { repository.duplicate(exercise) }
    fun moveToDay(exercise: PlanExercise, dayOfWeek: Int) = launch {
        repository.moveToDay(exercise, dayOfWeek)
        _messages.send(TrainingMessage("Moved ${exercise.name} to ${dayName(dayOfWeek)}"))
    }
    fun saveDay(day: PlanDay) = launch { repository.saveDay(day) }
    fun renamePlan(title: String) = launch { repository.renamePlan(title) }
    fun copyDay(from: Int, to: Int) = launch {
        val before = repository.copyDay(from, to)
        _messages.send(TrainingMessage("Copied ${dayName(from)} to ${dayName(to)}", undo = before))
    }
    fun clearDay(dayOfWeek: Int) = launch {
        val before = repository.clearDay(dayOfWeek)
        _messages.send(TrainingMessage("Cleared ${dayName(dayOfWeek)}", undo = before))
    }
    fun undo(snapshot: DaySnapshot) = launch { repository.restore(snapshot) }
    fun createEmptyPlan() = launch { repository.createEmptyPlan() }
    fun deletePlan() = launch { repository.deletePlan() }

    // ---- AI tools ----

    fun openDayAi(dayOfWeek: Int) {
        _dayAi.value = DayAiState(dayOfWeek)
    }

    fun closeDayAi() {
        if (_dayAi.value?.isLoading != true) _dayAi.value = null
    }

    /** Rewrites one day following [instruction]; on success the dialog closes and Undo is offered. */
    fun editDayWithAi(dayOfWeek: Int, instruction: String) {
        if (_dayAi.value?.isLoading == true || instruction.isBlank()) return
        _dayAi.value = DayAiState(dayOfWeek, isLoading = true)
        runDayEdit(dayOfWeek, instruction, success = "${dayName(dayOfWeek)} updated") { error ->
            _dayAi.update { it?.copy(isLoading = false, error = error) }
        }
    }

    /** Replaces one exercise with an AI-suggested alternative, leaving the rest of the day alone. */
    fun swapWithAi(exercise: PlanExercise) {
        if (_dayAi.value?.isLoading == true) return
        _dayAi.value = null
        val instruction = "Replace \"${exercise.name}\" with one suitable alternative exercise that trains the same " +
            "muscles with a similar sets and reps scheme. Keep every other exercise exactly the same and in the same order."
        swapping.value = exercise.id
        runDayEdit(exercise.dayOfWeek, instruction, success = "Swapped ${exercise.name}") { error ->
            launch { _messages.send(TrainingMessage(error)) }
        }
    }

    private fun runDayEdit(dayOfWeek: Int, instruction: String, success: String, onError: (String) -> Unit) {
        val state = uiState.value
        viewModelScope.launch {
            val profile = calories.profileSummary()
            val result = withContext(Dispatchers.IO) {
                geminiClient.editDay(
                    instruction = instruction,
                    dayOfWeek = DayOfWeek.of(dayOfWeek),
                    currentDay = describeDay(state, dayOfWeek),
                    restOfWeek = (1..7).filter { it != dayOfWeek }.joinToString("\n") { "${DayOfWeek.of(it).name}: ${summarizeDay(state, it)}" },
                    profileContext = profile,
                )
            }
            swapping.value = null
            when (result) {
                is GeminiResult.Error -> onError(result.message)
                is GeminiResult.Success -> {
                    val before = repository.replaceDay(result.value)
                    _dayAi.value = null
                    _messages.send(TrainingMessage(success, undo = before))
                }
            }
        }
    }

    private fun describeDay(state: TrainingUiState, dayOfWeek: Int): String {
        val day = state.days[dayOfWeek]
        val exercises = state.exercisesByDay[dayOfWeek].orEmpty()
        val header = "Title: ${day?.title ?: "Rest"}${if (day?.isRest != false && exercises.isEmpty()) " (rest day)" else ""}"
        return (listOf(header) + exercises.mapIndexed { i, ex ->
            "${i + 1}. ${ex.name} | sets: ${ex.sets ?: "null"} | target: ${ex.target} | notes: ${ex.notes}"
        }).joinToString("\n")
    }

    private fun summarizeDay(state: TrainingUiState, dayOfWeek: Int): String {
        val day = state.days[dayOfWeek]
        val exercises = state.exercisesByDay[dayOfWeek].orEmpty()
        if (exercises.isEmpty()) return day?.title ?: "Rest"
        return "${day?.title ?: "Training"} (" + exercises.joinToString(", ") { "${it.name} ${formatPrescription(it.sets, it.target)}".trim() } + ")"
    }

    private fun dayName(dayOfWeek: Int) = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.getDefault())

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
