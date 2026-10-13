package com.example.calorietracker

import android.app.Application
import com.example.calorietracker.account.AccountStore
import com.example.calorietracker.account.GoogleAuth
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.data.TrainingRepository
import com.example.calorietracker.network.AiRateLimiter
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.network.PrefsDailyCountStore
import com.example.calorietracker.notifications.ReminderStore
import com.example.calorietracker.notifications.Reminders

/** Holds the app-wide singletons (simple manual dependency injection). */
class CalorieTrackerApp : Application() {
    val accountStore: AccountStore by lazy { AccountStore(this) }
    val googleAuth: GoogleAuth by lazy { GoogleAuth(BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    val reminderStore: ReminderStore by lazy { ReminderStore(this) }

    private val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: CalorieRepository by lazy { CalorieRepository(database.calorieDao()) }
    val trainingRepository: TrainingRepository by lazy { TrainingRepository(database.trainingDao()) }
    val geminiClient: GeminiClient by lazy {
        GeminiClient(BuildConfig.GEMINI_API_KEY, AiRateLimiter(PrefsDailyCountStore(this)))
    }

    override fun onCreate() {
        super.onCreate()
        // Keep the hourly reminder check scheduled (or cancelled) to match the user's settings.
        Reminders.schedule(this, reminderStore.settings.value)
    }
}
