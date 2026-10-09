package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("Breakfast", "🍳"),
    LUNCH("Lunch", "🍛"),
    DINNER("Dinner", "🍲"),
    SNACK("Snack", "🍎"),
}

@Entity(tableName = "meals", indices = [Index("epochDay")])
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The day the meal belongs to (LocalDate.toEpochDay()). */
    val epochDay: Long,
    /** When the meal was logged, used to sort newest first. */
    val createdAtMillis: Long,
    val name: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val mealType: MealType,
    val note: String = "",
)

/** Total calories for one day, used by the weekly chart. */
data class DayCalories(
    val epochDay: Long,
    val calories: Int,
)
