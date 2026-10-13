package com.example.calorietracker.notifications

import com.example.calorietracker.data.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderPolicyTest {

    private val allOn = ReminderSettings(water = true, meals = true, workout = true)
    private val day = LocalDateTime.of(2026, 10, 13, 0, 0)
    private fun at(hour: Int, minute: Int = 0) = day.withHour(hour).withMinute(minute)

    private val nothingDone = DayStatus(
        waterMl = 0, waterTargetMl = 3500, mealTypesLogged = emptySet(),
        workoutTitle = "Lower Body A", workoutTotal = 4, workoutDone = 0,
    )

    private fun types(at: LocalDateTime, status: DayStatus = nothingDone, sent: SentLog = SentLog(), settings: ReminderSettings = allOn) =
        ReminderPolicy.decide(at, settings, status, sent).first.map { it.type }

    @Test
    fun quietHours_nothingBefore8OrAfter10() {
        assertTrue(types(at(7, 30)).isEmpty())
        assertTrue(types(at(22, 15)).isEmpty())
    }

    @Test
    fun water_onlyWhenBehindAndNotTooOften() {
        // 2 PM: expected ≈ 1.5 L, had 0 → remind.
        assertEquals(listOf(ReminderType.WATER), types(at(14), status = nothingDone.copy(mealTypesLogged = setOf(MealType.LUNCH))).filter { it == ReminderType.WATER })
        // On track (2 L by 2 PM) → no water reminder.
        assertTrue(ReminderType.WATER !in types(at(14), status = nothingDone.copy(waterMl = 2000)))
        // Reminded an hour ago → wait.
        assertTrue(ReminderType.WATER !in types(at(14), sent = SentLog(lastWaterAt = at(13))))
        // Off → never.
        assertTrue(ReminderType.WATER !in types(at(14), settings = allOn.copy(water = false)))
    }

    @Test
    fun meals_oncePerWindowWhenNotLogged() {
        val (first, log) = ReminderPolicy.decide(at(14, 10), allOn.copy(water = false), nothingDone, SentLog())
        assertEquals(listOf("Log your lunch"), first.map { it.title })
        // Same window an hour later: already sent today.
        val (second, _) = ReminderPolicy.decide(at(15, 10), allOn.copy(water = false), nothingDone, log)
        assertTrue(second.isEmpty())
        // Lunch already logged: no reminder.
        assertTrue(types(at(14), status = nothingDone.copy(mealTypesLogged = setOf(MealType.LUNCH)), settings = allOn.copy(water = false)).isEmpty())
    }

    @Test
    fun workout_inTheEveningIfUnfinished() {
        val evening = allOn.copy(water = false, meals = false)
        assertEquals(listOf(ReminderType.WORKOUT), types(at(18, 5), settings = evening))
        assertTrue(types(at(18, 5), status = nothingDone.copy(workoutDone = 4), settings = evening).isEmpty())
        assertTrue(types(at(18, 5), status = nothingDone.copy(workoutTotal = 0), settings = evening).isEmpty()) // rest day
        assertTrue(types(at(12), settings = evening).isEmpty()) // too early
    }

    @Test
    fun oldKeysAreDropped() {
        val yesterdayKey = "meal:LUNCH:${day.toLocalDate().minusDays(1).toEpochDay()}"
        val (_, log) = ReminderPolicy.decide(at(9), allOn, nothingDone, SentLog(keys = setOf(yesterdayKey)))
        assertTrue(yesterdayKey !in log.keys)
    }
}
