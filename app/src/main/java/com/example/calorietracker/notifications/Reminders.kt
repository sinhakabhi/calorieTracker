package com.example.calorietracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.calorietracker.CalorieTrackerApp
import com.example.calorietracker.MainActivity
import com.example.calorietracker.R
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Schedules the hourly reminder check and posts notifications. */
object Reminders {
    private const val CHANNEL_ID = "reminders"
    private const val WORK_NAME = "reminder-check"

    /** Starts the hourly check if any reminder is on, otherwise stops it. */
    fun schedule(context: Context, settings: ReminderSettings) {
        val work = WorkManager.getInstance(context)
        if (settings.anyEnabled) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS).build()
            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            work.cancelUniqueWork(WORK_NAME)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context, reminder: Reminder) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Water, meal logging and workout reminders"
            }
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // One notification per type: a newer water reminder replaces the old one.
        @Suppress("MissingPermission") // checked by canNotify()
        NotificationManagerCompat.from(context).notify(reminder.type.ordinal, notification)
    }
}

/** Runs about once an hour: reads today's data, asks [ReminderPolicy] what to send, sends it. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CalorieTrackerApp
        val settings = app.reminderStore.settings.value
        if (!settings.anyEnabled) return Result.success()

        val today = LocalDate.now()
        val targets = app.repository.dailyTargets.first()
        val meals = app.repository.mealsForDay(today).first()
        val exercises = app.trainingRepository.exercises.first().filter { it.dayOfWeek == today.dayOfWeek.value }
        val done = app.trainingRepository.completions(today, today).first().map { it.exerciseId }.toSet()
        val dayTitle = app.trainingRepository.days.first().firstOrNull { it.dayOfWeek == today.dayOfWeek.value }?.title

        val status = DayStatus(
            waterMl = app.repository.waterForDay(today).first(),
            waterTargetMl = targets?.waterMl,
            mealTypesLogged = meals.map { it.mealType }.toSet(),
            workoutTitle = dayTitle,
            workoutTotal = exercises.size,
            workoutDone = exercises.count { it.id in done },
        )
        val (reminders, sent) = ReminderPolicy.decide(LocalDateTime.now(), settings, status, app.reminderStore.readSentLog())
        reminders.forEach { Reminders.post(applicationContext, it) }
        app.reminderStore.writeSentLog(sent)
        return Result.success()
    }
}
