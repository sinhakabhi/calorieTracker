package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Total water drunk on one day; [epochDay] is LocalDate.toEpochDay(). */
@Entity(tableName = "water_intake")
data class WaterIntake(
    @PrimaryKey val epochDay: Long,
    val ml: Int,
)
