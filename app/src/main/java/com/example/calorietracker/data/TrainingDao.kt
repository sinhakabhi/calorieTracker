package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {

    @Query("SELECT * FROM plan_info WHERE id = 0")
    fun observePlanInfo(): Flow<PlanInfo?>

    @Query("SELECT * FROM plan_days ORDER BY dayOfWeek")
    fun observeDays(): Flow<List<PlanDay>>

    @Query("SELECT * FROM plan_exercises ORDER BY dayOfWeek, position, id")
    fun observeExercises(): Flow<List<PlanExercise>>

    @Query("SELECT * FROM exercise_completions WHERE epochDay BETWEEN :fromDay AND :toDay")
    fun observeCompletions(fromDay: Long, toDay: Long): Flow<List<ExerciseCompletion>>

    @Upsert
    suspend fun upsertPlanInfo(info: PlanInfo)

    @Upsert
    suspend fun upsertDays(days: List<PlanDay>)

    @Upsert
    suspend fun upsertDay(day: PlanDay)

    @Upsert
    suspend fun upsertExercise(exercise: PlanExercise)

    @Insert
    suspend fun insertExercises(exercises: List<PlanExercise>)

    @Query("DELETE FROM plan_exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("SELECT * FROM plan_exercises WHERE dayOfWeek = :dayOfWeek ORDER BY position, id")
    suspend fun exercisesForDay(dayOfWeek: Int): List<PlanExercise>

    @Query("SELECT * FROM plan_days WHERE dayOfWeek = :dayOfWeek")
    suspend fun getDay(dayOfWeek: Int): PlanDay?

    @Query("DELETE FROM plan_exercises WHERE dayOfWeek = :dayOfWeek")
    suspend fun deleteExercisesForDay(dayOfWeek: Int)

    @Query("UPDATE plan_exercises SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("UPDATE plan_info SET title = :title WHERE id = 0")
    suspend fun updatePlanTitle(title: String)

    /** Saves the order of a day's exercises: [orderedIds] first to last. */
    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }

    /** Replaces one day's title, rest flag and exercises in a single transaction. */
    @Transaction
    suspend fun replaceDay(day: PlanDay, exercises: List<PlanExercise>) {
        deleteExercisesForDay(day.dayOfWeek)
        upsertDay(day)
        exercises.forEach { upsertExercise(it) }
    }

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM plan_exercises WHERE dayOfWeek = :dayOfWeek")
    suspend fun nextPosition(dayOfWeek: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: ExerciseCompletion)

    @Query("DELETE FROM exercise_completions WHERE epochDay = :epochDay AND exerciseId = :exerciseId")
    suspend fun deleteCompletion(epochDay: Long, exerciseId: Long)

    @Query("DELETE FROM plan_exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM plan_days")
    suspend fun deleteAllDays()

    @Query("DELETE FROM plan_info")
    suspend fun deletePlanInfo()

    /** Deletes the whole plan. Completions go with their exercises (foreign key cascade). */
    @Transaction
    suspend fun deletePlan() {
        deleteAllExercises()
        deleteAllDays()
        deletePlanInfo()
    }

    /** Replaces the current plan with a new one in a single transaction. */
    @Transaction
    suspend fun replacePlan(info: PlanInfo, days: List<PlanDay>, exercises: List<PlanExercise>) {
        deletePlan()
        upsertPlanInfo(info)
        upsertDays(days)
        insertExercises(exercises)
    }
}
