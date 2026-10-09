package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One weight reading per day; [epochDay] is LocalDate.toEpochDay(). */
@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val epochDay: Long,
    val weightKg: Double,
)
