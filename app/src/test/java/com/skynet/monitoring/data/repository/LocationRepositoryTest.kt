package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.LocationRequest
import com.skynet.monitoring.data.api.model.MessageResponse
import com.skynet.monitoring.util.ApiException
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class LocationRepositoryTest {

    private val api = mockk<ApiService>()
    private val repo = LocationRepositoryImpl(api, Gson())

    @Test
    fun `sendLocation sukses`() = runTest {
        coEvery { api.sendLocation(any()) } returns Response.success(MessageResponse("Lokasi dicatat"))
        assertTrue(repo.sendLocation(5, -6.26, 107.0, "2026-06-11T14:05:30+07:00").isSuccess)
    }

    @Test
    fun `sendLocation meneruskan report_id dan recorded_at ke payload`() = runTest {
        val bodySlot = slot<LocationRequest>()
        coEvery { api.sendLocation(capture(bodySlot)) } returns Response.success(MessageResponse("ok"))
        repo.sendLocation(5, -6.26, 107.0, "2026-06-11T14:05:30+07:00")
        assertEquals(5, bodySlot.captured.reportId)
        assertEquals("2026-06-11T14:05:30+07:00", bodySlot.captured.recordedAt)
    }

    @Test
    fun `sendLocation 403 menjadi kegagalan dengan kode 403`() = runTest {
        val body = """{"message":"Tidak dapat mengirim lokasi"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.sendLocation(any()) } returns Response.error(403, body)
        val result = repo.sendLocation(5, -6.26, 107.0, null)
        assertTrue(result.isFailure)
        assertEquals(403, (result.exceptionOrNull() as ApiException).code)
    }
}
