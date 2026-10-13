package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The active training plan (always id 0): one repeating week. Days live in [PlanDay] and
 * exercises in [PlanExercise].
 */
@Entity(tableName = "plan_info")
data class PlanInfo(
    @PrimaryKey val id: Int = 0,
    val title: String,
    /** What the user asked for, kept so they can see or reuse it. Empty for hand-made plans. */
    val prompt: String,
    /** Coach notes: progression, warm-up, recovery. */
    val notes: String,
    val createdAtMillis: Long,
)

/** One day of the weekly plan; [dayOfWeek] is 1 = Monday … 7 = Sunday (java.time.DayOfWeek). */
@Entity(tableName = "plan_days")
data class PlanDay(
    @PrimaryKey val dayOfWeek: Int,
    val title: String,
    val isRest: Boolean,
)

@Entity(tableName = "plan_exercises", indices = [Index("dayOfWeek")])
data class PlanExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    /** Order within the day. */
    val position: Int,
    val name: String,
    /** Number of sets, or null for continuous work (a run, a timed circuit). */
    val sets: Int?,
    /** Per-set target, e.g. "8-10 reps", "45 s", "500 m", "5 km easy pace". */
    val target: String,
    val notes: String = "",
)

/** An exercise marked done on a date. Removed automatically when its exercise is deleted. */
@Entity(
    tableName = "exercise_completions",
    primaryKeys = ["epochDay", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = PlanExercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExerciseCompletion(
    val epochDay: Long,
    val exerciseId: Long,
)
