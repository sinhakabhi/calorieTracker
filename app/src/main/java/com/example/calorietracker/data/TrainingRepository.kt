package com.example.calorietracker.data

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

/** A generated or hand-made week, before it is saved. */
data class PlanDraft(
    val title: String,
    val notes: String,
    val days: List<DayDraft>,
)

data class DayDraft(
    val dayOfWeek: DayOfWeek,
    val title: String,
    val isRest: Boolean,
    val exercises: List<ExerciseDraft>,
)

data class ExerciseDraft(
    val name: String,
    val sets: Int?,
    val target: String,
    val notes: String,
)

/** A day as it was before a change, so the change can be undone. */
data class DaySnapshot(val day: PlanDay, val exercises: List<PlanExercise>)

class TrainingRepository(private val dao: TrainingDao) {

    val planInfo: Flow<PlanInfo?> = dao.observePlanInfo()
    val days: Flow<List<PlanDay>> = dao.observeDays()
    val exercises: Flow<List<PlanExercise>> = dao.observeExercises()

    fun completions(from: LocalDate, to: LocalDate): Flow<List<ExerciseCompletion>> =
        dao.observeCompletions(from.toEpochDay(), to.toEpochDay())

    /** Saves [draft] as the active plan, replacing any existing one. Every weekday gets a row. */
    suspend fun savePlan(draft: PlanDraft, prompt: String) {
        val byDay = draft.days.associateBy { it.dayOfWeek }
        val days = DayOfWeek.entries.map { dow ->
            val day = byDay[dow]
            PlanDay(dow.value, day?.title ?: "Rest", day?.isRest ?: true)
        }
        val exercises = draft.days.flatMap { day ->
            day.exercises.mapIndexed { index, ex ->
                PlanExercise(
                    dayOfWeek = day.dayOfWeek.value,
                    position = index,
                    name = ex.name,
                    sets = ex.sets,
                    target = ex.target,
                    notes = ex.notes,
                )
            }
        }
        dao.replacePlan(
            PlanInfo(title = draft.title, prompt = prompt, notes = draft.notes, createdAtMillis = System.currentTimeMillis()),
            days,
            exercises,
        )
    }

    /** Starts an empty plan the user fills in by hand. */
    suspend fun createEmptyPlan() = savePlan(PlanDraft("My training week", "", emptyList()), prompt = "")

    suspend fun deletePlan() = dao.deletePlan()

    suspend fun saveDay(day: PlanDay) = dao.upsertDay(day)

    /** Adds a new exercise at the end of its day, or updates an existing one. */
    suspend fun saveExercise(exercise: PlanExercise) {
        if (exercise.id == 0L) {
            dao.upsertExercise(exercise.copy(position = dao.nextPosition(exercise.dayOfWeek)))
        } else {
            dao.upsertExercise(exercise)
        }
    }

    suspend fun deleteExercise(id: Long) = dao.deleteExercise(id)

    suspend fun renamePlan(title: String) = dao.updatePlanTitle(title)

    /** Moves [exercise] up (delta −1) or down (+1) within its day. */
    suspend fun moveExercise(exercise: PlanExercise, delta: Int) {
        val ids = dao.exercisesForDay(exercise.dayOfWeek).map { it.id }.toMutableList()
        val from = ids.indexOf(exercise.id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        dao.reorder(ids)
    }

    /** Moves [exercise] to the end of another day. Its check-offs stay with it. */
    suspend fun moveToDay(exercise: PlanExercise, dayOfWeek: Int) {
        if (exercise.dayOfWeek == dayOfWeek) return
        dao.upsertExercise(exercise.copy(dayOfWeek = dayOfWeek, position = dao.nextPosition(dayOfWeek)))
        markTrainingDay(dayOfWeek)
    }

    /** Adds a copy of [exercise] right after it. */
    suspend fun duplicate(exercise: PlanExercise) {
        val ids = dao.exercisesForDay(exercise.dayOfWeek).map { it.id }.toMutableList()
        dao.upsertExercise(exercise.copy(id = 0, position = dao.nextPosition(exercise.dayOfWeek)))
        val newId = dao.exercisesForDay(exercise.dayOfWeek).maxOf { it.id }
        ids.add(ids.indexOf(exercise.id) + 1, newId)
        dao.reorder(ids)
    }

    suspend fun snapshot(dayOfWeek: Int): DaySnapshot? =
        dao.getDay(dayOfWeek)?.let { DaySnapshot(it, dao.exercisesForDay(dayOfWeek)) }

    /** Puts a day back exactly as it was (same exercise ids). */
    suspend fun restore(snapshot: DaySnapshot) = dao.replaceDay(snapshot.day, snapshot.exercises)

    /** Replaces a day with [draft] (e.g. an AI edit). Returns the previous state for undo. */
    suspend fun replaceDay(draft: DayDraft): DaySnapshot? {
        val before = snapshot(draft.dayOfWeek.value)
        val dow = draft.dayOfWeek.value
        dao.replaceDay(
            PlanDay(dow, draft.title, draft.isRest),
            draft.exercises.mapIndexed { index, ex ->
                PlanExercise(dayOfWeek = dow, position = index, name = ex.name, sets = ex.sets, target = ex.target, notes = ex.notes)
            },
        )
        return before
    }

    /** Copies a day's title and exercises onto another day, replacing what was there. Returns the old target day. */
    suspend fun copyDay(fromDayOfWeek: Int, toDayOfWeek: Int): DaySnapshot? {
        val source = snapshot(fromDayOfWeek) ?: return null
        val before = snapshot(toDayOfWeek)
        dao.replaceDay(
            source.day.copy(dayOfWeek = toDayOfWeek),
            source.exercises.map { it.copy(id = 0, dayOfWeek = toDayOfWeek) },
        )
        return before
    }

    /** Removes every exercise from a day. Returns the previous state for undo. */
    suspend fun clearDay(dayOfWeek: Int): DaySnapshot? {
        val before = snapshot(dayOfWeek)
        dao.deleteExercisesForDay(dayOfWeek)
        return before
    }

    /** A day that receives exercises is no longer a rest day. */
    private suspend fun markTrainingDay(dayOfWeek: Int) {
        val day = dao.getDay(dayOfWeek) ?: return
        if (day.isRest) dao.upsertDay(day.copy(isRest = false, title = if (day.title == "Rest") "Training" else day.title))
    }

    suspend fun setCompleted(date: LocalDate, exerciseId: Long, done: Boolean) {
        if (done) {
            dao.insertCompletion(ExerciseCompletion(date.toEpochDay(), exerciseId))
        } else {
            dao.deleteCompletion(date.toEpochDay(), exerciseId)
        }
    }
}
