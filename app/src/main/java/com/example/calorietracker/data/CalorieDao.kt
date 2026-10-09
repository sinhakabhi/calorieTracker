package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieDao {

    // Profile

    @Query("SELECT * FROM profile WHERE id = 0")
    fun observeProfile(): Flow<Profile?>

    @Upsert
    suspend fun upsertProfile(profile: Profile)

    // Weight history

    @Query("SELECT * FROM weight_entries ORDER BY epochDay DESC")
    fun observeWeightHistory(): Flow<List<WeightEntry>>

    @Upsert
    suspend fun upsertWeight(entry: WeightEntry)

    @Delete
    suspend fun deleteWeight(entry: WeightEntry)

    // Water

    @Query("SELECT ml FROM water_intake WHERE epochDay = :epochDay")
    fun observeWaterMl(epochDay: Long): Flow<Int?>

    @Query("SELECT ml FROM water_intake WHERE epochDay = :epochDay")
    suspend fun getWaterMl(epochDay: Long): Int?

    @Upsert
    suspend fun upsertWater(entry: WaterIntake)

    /** Adds [deltaMl] (negative to remove) to the day's total, never going below 0. */
    @Transaction
    suspend fun addWater(epochDay: Long, deltaMl: Int) {
        val current = getWaterMl(epochDay) ?: 0
        upsertWater(WaterIntake(epochDay, (current + deltaMl).coerceAtLeast(0)))
    }

    // Meals

    @Query("SELECT * FROM meals WHERE epochDay = :epochDay ORDER BY createdAtMillis DESC")
    fun observeMealsForDay(epochDay: Long): Flow<List<Meal>>

    @Query(
        """
        SELECT epochDay, SUM(calories) AS calories FROM meals
        WHERE epochDay BETWEEN :fromDay AND :toDay
        GROUP BY epochDay
        """
    )
    fun observeDailyCalories(fromDay: Long, toDay: Long): Flow<List<DayCalories>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMeal(id: Long): Meal?

    @Upsert
    suspend fun upsertMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)
}
