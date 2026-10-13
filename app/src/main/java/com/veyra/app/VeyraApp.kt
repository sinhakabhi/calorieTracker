package com.veyra.app

import android.app.Application
import com.veyra.app.access.InviteChecker
import com.veyra.app.account.AccountStore
import com.veyra.app.account.GoogleAuth
import com.veyra.app.data.AppDatabase
import com.veyra.app.data.CalorieRepository
import com.veyra.app.data.TrainingRepository
import com.veyra.app.network.AiRateLimiter
import com.veyra.app.network.GeminiClient
import com.veyra.app.network.PrefsDailyCountStore
import com.veyra.app.notifications.ReminderStore
import com.veyra.app.notifications.Reminders

/** Holds the app-wide singletons (simple manual dependency injection). */
class VeyraApp : Application() {
    val accountStore: AccountStore by lazy { AccountStore(this) }
    val googleAuth: GoogleAuth by lazy { GoogleAuth(googleWebClientId()) }
    val inviteChecker: InviteChecker by lazy { InviteChecker(this, enabled = BuildConfig.INVITE_ONLY) }
    val reminderStore: ReminderStore by lazy { ReminderStore(this) }

    private val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: CalorieRepository by lazy { CalorieRepository(database.calorieDao()) }
    val trainingRepository: TrainingRepository by lazy { TrainingRepository(database.trainingDao()) }
    val geminiClient: GeminiClient by lazy {
        GeminiClient(BuildConfig.GEMINI_API_KEY, AiRateLimiter(PrefsDailyCountStore(this)))
    }

    /**
     * The Web client ID for Google sign-in: from local.properties, or else the one the Google
     * services plugin generates from google-services.json (when Google sign-in is enabled in Firebase).
     */
    private fun googleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID.ifBlank {
        @Suppress("DiscouragedApi") // the resource only exists when google-services.json does
        val id = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (id != 0) getString(id) else ""
    }

    override fun onCreate() {
        super.onCreate()
        // Keep the hourly reminder check scheduled (or cancelled) to match the user's settings.
        Reminders.schedule(this, reminderStore.settings.value)
    }
}
