package com.skynet.monitoring.util

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkDurationTest {

    @Test
    fun `elapsedSeconds menghitung selisih detik`() {
        assertEquals(6L, elapsedSeconds(startMillis = 4_000, nowMillis = 10_000))
    }

    @Test
    fun `elapsedSeconds tak pernah negatif`() {
        assertEquals(0L, elapsedSeconds(startMillis = 10_000, nowMillis = 4_000))
    }

    @Test
    fun `flow memancarkan elapsed dari start lalu bertambah tiap tick`() = runTest {
        // nowProvider ditambat ke jam virtual runTest agar deterministik.
        elapsedSecondsFlow(
            startMillis = 0L,
            nowProvider = { testScheduler.currentTime },
            tickMillis = 1000L,
        ).test {
            assertEquals(0L, awaitItem()) // emisi pertama langsung, t=0
            assertEquals(1L, awaitItem()) // setelah delay 1000 → now=1000
            assertEquals(2L, awaitItem()) // now=2000
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow tanpa start mulai dari nol`() = runTest {
        elapsedSecondsFlow(
            startMillis = null,
            nowProvider = { testScheduler.currentTime },
            tickMillis = 1000L,
        ).test {
            assertEquals(0L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
