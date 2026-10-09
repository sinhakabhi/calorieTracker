package com.example.calorietracker

import android.app.Application
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.CalorieRepository
import com.example.calorietracker.network.AiRateLimiter
import com.example.calorietracker.network.GeminiClient
import com.example.calorietracker.network.PrefsDailyCountStore

/** Holds the app-wide singletons (simple manual dependency injection). */
class CalorieTrackerApp : Application() {
    val repository: CalorieRepository by lazy {
        CalorieRepository(AppDatabase.create(this).calorieDao())
    }
    val geminiClient: GeminiClient by lazy {
        GeminiClient(BuildConfig.GEMINI_API_KEY, AiRateLimiter(PrefsDailyCountStore(this)))
    }
}
