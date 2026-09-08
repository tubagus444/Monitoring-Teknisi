package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.MessageResponse
import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.api.model.NotificationListResponse
import com.skynet.monitoring.data.local.room.dao.NotificationDao
import com.skynet.monitoring.data.local.room.entity.toEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NotificationRepositoryTest {

    private val api = mockk<ApiService>()
    private val notificationDao = mockk<NotificationDao>(relaxed = true)
    private val repo = NotificationRepositoryImpl(api, notificationDao, Gson())

    private val item = NotificationItem(1, "Tugas Baru", "Anda ditugaskan", isRead = false, createdAt = null)

    @Test
    fun `getNotifications membuka pembungkus data dan menyimpan ke cache`() = runTest {
        coEvery { api.getNotifications() } returns
            Response.success(NotificationListResponse(listOf(item)))
        val result = repo.getNotifications()
        assertEquals(listOf(item), result.getOrNull())
        coVerify(exactly = 1) { notificationDao.insertNotifications(listOf(item.toEntity())) }
    }

    @Test
    fun `getNotifications fallback ke cache lokal saat offline`() = runTest {
        coEvery { api.getNotifications() } throws IOException("No internet")
        coEvery { notificationDao.getNotifications() } returns listOf(item.toEntity())

        val result = repo.getNotifications()
        assertTrue(result.isSuccess)
        assertEquals(listOf(item), result.getOrNull())
    }

    @Test
    fun `getNotifications gagal jika jaringan error dan cache lokal kosong`() = runTest {
        coEvery { api.getNotifications() } throws IOException("No internet")
        coEvery { notificationDao.getNotifications() } returns emptyList()

        val result = repo.getNotifications()
        assertTrue(result.isFailure)
        assertEquals("Periksa koneksi internet Anda", result.exceptionOrNull()?.message)
    }

    @Test
    fun `markRead sukses menandai lokal dan mengembalikan Unit`() = runTest {
        coEvery { api.markNotificationRead(1) } returns Response.success(MessageResponse("ok"))
        val result = repo.markRead(1)
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { notificationDao.markAsRead(1) }
    }

    @Test
    fun `getNotifications meneruskan error 401 tanpa fallback ke cache`() = runTest {
        val body = """{"message":"Sesi berakhir"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.getNotifications() } returns Response.error(401, body)
        val result = repo.getNotifications()
        assertTrue(result.isFailure)
        assertEquals("Sesi berakhir", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { notificationDao.getNotifications() }
    }
}
