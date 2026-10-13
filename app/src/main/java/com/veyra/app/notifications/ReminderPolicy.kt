package com.veyra.app.notifications

import com.veyra.app.data.MealType
import java.time.Duration
import java.time.LocalDateTime

enum class ReminderType { WATER, MEAL, WORKOUT }

data class Reminder(val type: ReminderType, val title: String, val text: String)

/** Which reminders the user turned on. */
data class ReminderSettings(
    val water: Boolean = false,
    val meals: Boolean = false,
    val workout: Boolean = false,
) {
    val anyEnabled: Boolean get() = water || meals || workout
}

/** What the user has done today, as far as reminders care. */
data class DayStatus(
    val waterMl: Int,
    val waterTargetMl: Int?,
    val mealTypesLogged: Set<MealType>,
    val workoutTitle: String?,
    val workoutTotal: Int,
    val workoutDone: Int,
)

/** What was already sent, so nothing repeats: last water reminder time and today's one-off keys. */
data class SentLog(
    val lastWaterAt: LocalDateTime? = null,
    val keys: Set<String> = emptySet(),
)

/**
 * Decides which reminders to send at [now]. Pure logic, so it can be unit tested; the
 * worker runs it about once an hour. Nothing is sent outside 8:00–22:00.
 */
object ReminderPolicy {

    const val WATER_INTERVAL_HOURS = 2L
    private const val DAY_START_HOUR = 8
    private const val DAY_END_HOUR = 22

    /** Meal reminder windows: a reminder is sent once if that meal isn't logged by then. */
    private val mealWindows = listOf(
        Triple(MealType.BREAKFAST, 10..11, "Log your breakfast"),
        Triple(MealType.LUNCH, 14..15, "Log your lunch"),
        Triple(MealType.DINNER, 21..21, "Log your dinner"),
    )
    private val workoutWindow = 18..20

    fun decide(now: LocalDateTime, settings: ReminderSettings, status: DayStatus, sent: SentLog): Pair<List<Reminder>, SentLog> {
        if (now.hour !in DAY_START_HOUR until DAY_END_HOUR) return emptyList<Reminder>() to sent
        val day = now.toLocalDate().toEpochDay()
        val reminders = mutableListOf<Reminder>()
        var lastWater = sent.lastWaterAt
        // Keep only today's keys so the log doesn't grow forever.
        val keys = sent.keys.filter { it.endsWith(":$day") }.toMutableSet()

        if (settings.water && status.waterTargetMl != null) {
            val dueAgain = lastWater == null || Duration.between(lastWater, now).toHours() >= WATER_INTERVAL_HOURS
            // Where the user "should" be by now if they spread the target evenly over the day.
            val dayFraction = ((now.hour - DAY_START_HOUR) + now.minute / 60.0) / (DAY_END_HOUR - DAY_START_HOUR)
            val expectedMl = (status.waterTargetMl * dayFraction).toInt()
            if (dueAgain && status.waterMl < status.waterTargetMl && status.waterMl + 250 < expectedMl) {
                reminders += Reminder(
                    ReminderType.WATER,
                    "Time for some water",
                    "You've had ${litres(status.waterMl)} of ${litres(status.waterTargetMl)} L today.",
                )
                lastWater = now
            }
        }

        if (settings.meals) {
            for ((type, hours, title) in mealWindows) {
                val key = "meal:${type.name}:$day"
                if (now.hour in hours && type !in status.mealTypesLogged && key !in keys) {
                    reminders += Reminder(ReminderType.MEAL, title, "Keep your day accurate: log what you had for ${type.label.lowercase()}.")
                    keys += key
                }
            }
        }

        if (settings.workout && status.workoutTotal > 0 && status.workoutDone < status.workoutTotal) {
            val key = "workout:$day"
            if (now.hour in workoutWindow && key !in keys) {
                val title = status.workoutTitle ?: "Today's workout"
                reminders += Reminder(
                    ReminderType.WORKOUT,
                    "$title is waiting",
                    "${status.workoutDone} of ${status.workoutTotal} exercises done today.",
                )
                keys += key
            }
        }

        return reminders to SentLog(lastWater, keys)
    }

    private fun litres(ml: Int) = java.math.BigDecimal(ml).movePointLeft(3).stripTrailingZeros().toPlainString()
}
