package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.MessageResponse
import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.api.model.NotificationListResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class NotificationRepositoryTest {

    private val api = mockk<ApiService>()
    private val repo = NotificationRepositoryImpl(api, Gson())

    @Test
    fun `getNotifications membuka pembungkus data`() = runTest {
        val item = NotificationItem(1, "Tugas Baru", "Anda ditugaskan", isRead = false, createdAt = null)
        coEvery { api.getNotifications() } returns
            Response.success(NotificationListResponse(listOf(item)))
        assertEquals(listOf(item), repo.getNotifications().getOrNull())
    }

    @Test
    fun `markRead sukses mengembalikan Unit`() = runTest {
        coEvery { api.markNotificationRead(1) } returns Response.success(MessageResponse("ok"))
        assertTrue(repo.markRead(1).isSuccess)
    }

    @Test
    fun `getNotifications meneruskan error sebagai kegagalan`() = runTest {
        val body = """{"message":"Sesi berakhir"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.getNotifications() } returns Response.error(401, body)
        val result = repo.getNotifications()
        assertTrue(result.isFailure)
        assertEquals("Sesi berakhir", result.exceptionOrNull()?.message)
    }
}
