package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.calorietracker.domain.ActivityLevel
import com.example.calorietracker.domain.Goal
import com.example.calorietracker.domain.Sex

/** The single user profile (always id 0). Weight is stored separately in [WeightEntry]. */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 0,
    val age: Int,
    val sex: Sex,
    val heightCm: Double,
    val activityLevel: ActivityLevel,
    val goal: Goal,
)
