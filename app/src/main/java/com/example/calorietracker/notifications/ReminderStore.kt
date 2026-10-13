package com.example.calorietracker.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

/** Reminder on/off switches and what was already sent, in private SharedPreferences. */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    fun update(settings: ReminderSettings) {
        prefs.edit()
            .putBoolean("water", settings.water)
            .putBoolean("meals", settings.meals)
            .putBoolean("workout", settings.workout)
            .apply()
        _settings.value = settings
    }

    fun readSentLog(): SentLog = SentLog(
        lastWaterAt = prefs.getString("last_water", null)?.let(LocalDateTime::parse),
        keys = prefs.getStringSet("sent_keys", emptySet()).orEmpty(),
    )

    fun writeSentLog(log: SentLog) {
        prefs.edit()
            .putString("last_water", log.lastWaterAt?.toString())
            .putStringSet("sent_keys", log.keys)
            .apply()
    }

    private fun readSettings() = ReminderSettings(
        water = prefs.getBoolean("water", false),
        meals = prefs.getBoolean("meals", false),
        workout = prefs.getBoolean("workout", false),
    )
}
