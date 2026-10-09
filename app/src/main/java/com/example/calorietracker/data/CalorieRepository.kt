package com.example.calorietracker.data

import com.example.calorietracker.domain.CalorieCalculator
import com.example.calorietracker.domain.MacroTargets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class DailyTargets(val kcal: Int, val macros: MacroTargets, val waterMl: Int)

class CalorieRepository(private val dao: CalorieDao) {

    val profile: Flow<Profile?> = dao.observeProfile()

    /** Newest first. */
    val weightHistory: Flow<List<WeightEntry>> = dao.observeWeightHistory()

    /** Daily calorie, macro and water targets from the profile and the most recent weight, or null before onboarding. */
    val dailyTargets: Flow<DailyTargets?> = combine(profile, weightHistory) { profile, weights ->
        val latestWeight = weights.firstOrNull()
        if (profile == null || latestWeight == null) {
            null
        } else {
            val kcal = CalorieCalculator.dailyTarget(
                age = profile.age,
                sex = profile.sex,
                heightCm = profile.heightCm,
                weightKg = latestWeight.weightKg,
                activity = profile.activityLevel,
                goal = profile.goal,
            )
            DailyTargets(
                kcal = kcal,
                macros = CalorieCalculator.macroTargets(kcal, latestWeight.weightKg, profile.activityLevel),
                waterMl = CalorieCalculator.waterTargetMl(latestWeight.weightKg),
            )
        }
    }

    /** Saves the profile and records [weightKg] as today's weight. */
    suspend fun saveProfile(profile: Profile, weightKg: Double) {
        dao.upsertProfile(profile)
        dao.upsertWeight(WeightEntry(LocalDate.now().toEpochDay(), weightKg))
    }

    suspend fun deleteWeight(entry: WeightEntry) = dao.deleteWeight(entry)

    fun mealsForDay(date: LocalDate): Flow<List<Meal>> = dao.observeMealsForDay(date.toEpochDay())

    /** Calories for each of the 7 days ending on [endDate], oldest first, with 0 for empty days. */
    fun lastSevenDays(endDate: LocalDate): Flow<List<DayCalories>> {
        val from = endDate.minusDays(6).toEpochDay()
        val to = endDate.toEpochDay()
        return dao.observeDailyCalories(from, to).map { rows ->
            val byDay = rows.associate { it.epochDay to it.calories }
            (from..to).map { day -> DayCalories(day, byDay[day] ?: 0) }
        }
    }

    fun waterForDay(date: LocalDate): Flow<Int> = dao.observeWaterMl(date.toEpochDay()).map { it ?: 0 }

    suspend fun addWater(date: LocalDate, deltaMl: Int) = dao.addWater(date.toEpochDay(), deltaMl)

    suspend fun getMeal(id: Long): Meal? = dao.getMeal(id)

    suspend fun saveMeal(meal: Meal) = dao.upsertMeal(meal)

    suspend fun deleteMeal(meal: Meal) = dao.deleteMeal(meal)
}
