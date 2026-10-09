package com.example.calorietracker.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class AiRateLimiterTest {

    private class MemoryStore : DailyCountStore {
        var value: Pair<Long, Int>? = null
        override fun read() = value
        override fun write(epochDay: Long, count: Int) {
            value = epochDay to count
        }
    }

    private var clock = 1_000_000_000_000L // a fixed instant, advanced by each test
    private val store = MemoryStore()
    private val limiter = AiRateLimiter(store, now = { clock }, zone = ZoneOffset.UTC)

    @Test
    fun enforcesCooldownBetweenRequests() {
        assertNull(limiter.tryAcquire())
        clock += 1_000
        assertNotNull(limiter.tryAcquire())
        clock += AiRateLimiter.MIN_INTERVAL_MS
        assertNull(limiter.tryAcquire())
    }

    @Test
    fun enforcesPerMinuteCap() {
        repeat(AiRateLimiter.PER_MINUTE) {
            assertNull(limiter.tryAcquire())
            clock += AiRateLimiter.MIN_INTERVAL_MS
        }
        assertNotNull(limiter.tryAcquire())
        clock += 60_000
        assertNull(limiter.tryAcquire())
    }

    @Test
    fun enforcesDailyCapAndResetsNextDay() {
        val today = clock / 86_400_000
        store.value = today to AiRateLimiter.PER_DAY
        assertNotNull(limiter.tryAcquire())

        clock += 86_400_000
        assertNull(limiter.tryAcquire())
        assertEquals(today + 1 to 1, store.value)
    }

    @Test
    fun blockedRequestsAreNotCounted() {
        assertNull(limiter.tryAcquire())
        assertNotNull(limiter.tryAcquire()) // cooldown
        assertEquals(1, store.value!!.second)
    }
}
