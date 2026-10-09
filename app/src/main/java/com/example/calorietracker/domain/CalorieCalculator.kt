package com.example.calorietracker.domain

import kotlin.math.roundToInt

enum class Sex(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
}

/**
 * [factor] multiplies BMR (calories burned at rest) to estimate what you burn in a whole day.
 * [proteinPerKg] is the daily protein target in grams per kg of body weight.
 */
enum class ActivityLevel(val factor: Double, val proteinPerKg: Double, val label: String, val description: String) {
    SEDENTARY(1.2, 1.0, "Sedentary", "Desk job, little or no exercise"),
    LIGHT(1.375, 1.2, "Lightly active", "Light exercise or walking 1–3 days a week"),
    MODERATE(1.55, 1.6, "Moderately active", "Exercise or sports 3–5 days a week"),
    VERY_ACTIVE(1.725, 1.8, "Very active", "Hard exercise 6–7 days a week or a physical job"),
}

enum class Goal(val adjustmentKcal: Int, val label: String) {
    LOSE(-500, "Lose"),
    MAINTAIN(0, "Maintain"),
    GAIN(300, "Gain"),
}

/** Daily macro targets in grams. */
data class MacroTargets(val proteinG: Int, val carbsG: Int, val fatG: Int)

/** Pure calorie maths, kept free of Android types so it can be unit tested. */
object CalorieCalculator {
    const val MIN_DAILY_KCAL = 1200

    val AGE_RANGE = 13..100
    val HEIGHT_RANGE_CM = 100.0..250.0
    val WEIGHT_RANGE_KG = 30.0..300.0

    /** Basal metabolic rate (kcal/day) using the Mifflin-St Jeor equation. */
    fun bmr(age: Int, sex: Sex, heightCm: Double, weightKg: Double): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    /** BMR × activity factor + goal adjustment, never below [MIN_DAILY_KCAL]. */
    fun dailyTarget(
        age: Int,
        sex: Sex,
        heightCm: Double,
        weightKg: Double,
        activity: ActivityLevel,
        goal: Goal,
    ): Int {
        val tdee = bmr(age, sex, heightCm, weightKg) * activity.factor
        return (tdee + goal.adjustmentKcal).roundToInt().coerceAtLeast(MIN_DAILY_KCAL)
    }

    /** Daily protein target in grams: body weight × the activity level's grams per kg. */
    fun proteinTarget(weightKg: Double, activity: ActivityLevel): Int =
        (weightKg * activity.proteinPerKg).roundToInt()

    /** Share of daily calories that comes from fat. */
    const val FAT_SHARE = 0.25

    /**
     * Protein from body weight, fat as [FAT_SHARE] of calories (9 kcal/g),
     * and carbs fill the remaining calories (4 kcal/g, never below 0).
     */
    fun macroTargets(dailyKcal: Int, weightKg: Double, activity: ActivityLevel): MacroTargets {
        val protein = proteinTarget(weightKg, activity)
        val fat = (dailyKcal * FAT_SHARE / 9).roundToInt()
        val carbs = ((dailyKcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return MacroTargets(proteinG = protein, carbsG = carbs, fatG = fat)
    }

    /** Daily water target in ml: 35 ml per kg of body weight, kept between 2 and 4 litres. */
    fun waterTargetMl(weightKg: Double): Int =
        ((weightKg * 35 / 50).roundToInt() * 50).coerceIn(2000, 4000)

    fun isValidAge(age: Int?): Boolean = age != null && age in AGE_RANGE
    fun isValidHeight(heightCm: Double?): Boolean = heightCm != null && heightCm in HEIGHT_RANGE_CM
    fun isValidWeight(weightKg: Double?): Boolean = weightKg != null && weightKg in WEIGHT_RANGE_KG
}
