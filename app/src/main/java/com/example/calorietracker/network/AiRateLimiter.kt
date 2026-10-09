package com.example.calorietracker.network

import android.content.Context
import java.time.Instant
import java.time.ZoneId

/** Where the daily request count is kept, so it survives app restarts. */
interface DailyCountStore {
    /** The stored (epochDay, count), or null if nothing is stored yet. */
    fun read(): Pair<Long, Int>?
    fun write(epochDay: Long, count: Int)
}

/**
 * Client-side limits on AI requests, kept below the free tier's limits so the app
 * never burns through the quota:
 * - a short cooldown between requests,
 * - at most [PER_MINUTE] requests in any 60 seconds,
 * - at most [PER_DAY] requests per calendar day (persisted).
 */
class AiRateLimiter(
    private val store: DailyCountStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    companion object {
        const val MIN_INTERVAL_MS = 3_000L
        const val PER_MINUTE = 5
        const val PER_DAY = 50
        private const val MINUTE_MS = 60_000L
    }

    private val recent = ArrayDeque<Long>()

    /**
     * Records a request if it is allowed and returns null; otherwise returns a message
     * explaining why it was blocked (and records nothing).
     */
    @Synchronized
    fun tryAcquire(): String? {
        val time = now()
        while (recent.isNotEmpty() && time - recent.first() >= MINUTE_MS) recent.removeFirst()

        recent.lastOrNull()?.let { last ->
            if (time - last < MIN_INTERVAL_MS) return "Please wait a few seconds before trying again."
        }
        if (recent.size >= PER_MINUTE) {
            val waitSeconds = (MINUTE_MS - (time - recent.first())) / 1000 + 1
            return "Too many AI requests in a minute. Try again in $waitSeconds s."
        }
        val today = Instant.ofEpochMilli(time).atZone(zone).toLocalDate().toEpochDay()
        val usedToday = store.read()?.takeIf { it.first == today }?.second ?: 0
        if (usedToday >= PER_DAY) {
            return "Daily AI limit of $PER_DAY requests reached. It resets tomorrow; you can still fill in the fields manually."
        }

        recent.addLast(time)
        store.write(today, usedToday + 1)
        return null
    }
}

/** [DailyCountStore] backed by SharedPreferences. */
class PrefsDailyCountStore(context: Context) : DailyCountStore {
    private val prefs = context.getSharedPreferences("ai_usage", Context.MODE_PRIVATE)

    override fun read(): Pair<Long, Int>? =
        if (prefs.contains("day")) prefs.getLong("day", 0) to prefs.getInt("count", 0) else null

    override fun write(epochDay: Long, count: Int) {
        prefs.edit().putLong("day", epochDay).putInt("count", count).apply()
    }
}
