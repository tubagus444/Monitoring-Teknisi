package com.skynet.monitoring.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.PhotoUploadResponse
import com.skynet.monitoring.data.api.model.RepairPhoto
import com.skynet.monitoring.data.api.model.StatusAction
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.TaskDetailResponse
import com.skynet.monitoring.data.api.model.TaskListResponse
import com.skynet.monitoring.data.api.model.UpdateStatusRequest
import com.skynet.monitoring.data.api.model.UpdateStatusResponse
import com.skynet.monitoring.data.local.room.dao.TaskDao
import com.skynet.monitoring.data.local.room.entity.toEntity
import com.skynet.monitoring.util.ImageCompressor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class TaskRepositoryTest {

    private val api = mockk<ApiService>()
    private val taskDao = mockk<TaskDao>(relaxed = true)
    private val imageCompressor = mockk<ImageCompressor>()
    private val repo = TaskRepositoryImpl(api, taskDao, Gson(), imageCompressor)

    private fun task(id: Int = 1) = Task(
        id = id,
        reportId = 5,
        status = "ditugaskan",
        customer = "Pak Ahmad",
        address = "Jl. Mawar No.3",
        damageType = "Kabel Putus",
        notes = null,
        assignedAt = null,
    )

    @Test
    fun `getTasks membuka pembungkus data dan menyimpan ke cache`() = runTest {
        coEvery { api.getTasks() } returns Response.success(TaskListResponse(listOf(task())))
        val result = repo.getTasks()
        assertEquals(listOf(task()), result.getOrNull())
        coVerify(exactly = 1) { taskDao.insertTasks(listOf(task().toEntity())) }
    }

    @Test
    fun `getTasks fallback ke cache lokal saat offline atau jaringan error`() = runTest {
        coEvery { api.getTasks() } throws IOException("No internet")
        coEvery { taskDao.getActiveTasks() } returns listOf(task().toEntity())

        val result = repo.getTasks()
        assertTrue(result.isSuccess)
        assertEquals(listOf(task()), result.getOrNull())
    }

    @Test
    fun `getTasks gagal jika jaringan error dan cache lokal kosong`() = runTest {
        coEvery { api.getTasks() } throws IOException("No internet")
        coEvery { taskDao.getActiveTasks() } returns emptyList()

        val result = repo.getTasks()
        assertTrue(result.isFailure)
        assertEquals("Periksa koneksi internet Anda", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getTasks error 401 tidak membaca cache lokal`() = runTest {
        val body = """{"message":"Unauthenticated"}""".toResponseBody("application/json".toMediaType())
        coEvery { api.getTasks() } returns Response.error(401, body)

        val result = repo.getTasks()
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { taskDao.getActiveTasks() }
    }

    @Test
    fun `getTaskHistory membuka pembungkus data dan menyimpan ke cache`() = runTest {
        val completedTask = task(2).copy(status = "selesai", completedAt = "2026-06-01T10:00:00+07:00")
        coEvery { api.getTasks("completed") } returns Response.success(TaskListResponse(listOf(completedTask)))
        val result = repo.getTaskHistory()
        assertEquals(listOf(completedTask), result.getOrNull())
        coVerify(exactly = 1) { taskDao.insertTasks(listOf(completedTask.toEntity())) }
    }

    @Test
    fun `getTaskHistory fallback ke cache lokal saat offline atau jaringan error`() = runTest {
        val completedTask = task(2).copy(status = "selesai", completedAt = "2026-06-01T10:00:00+07:00")
        coEvery { api.getTasks("completed") } throws IOException("No internet")
        coEvery { taskDao.getHistoryTasks() } returns listOf(completedTask.toEntity())

        val result = repo.getTaskHistory()
        assertTrue(result.isSuccess)
        assertEquals(listOf(completedTask), result.getOrNull())
    }

    @Test
    fun `getTaskHistory gagal jika jaringan error dan cache lokal kosong`() = runTest {
        coEvery { api.getTasks("completed") } throws IOException("No internet")
        coEvery { taskDao.getHistoryTasks() } returns emptyList()

        val result = repo.getTaskHistory()
        assertTrue(result.isFailure)
        assertEquals("Periksa koneksi internet Anda", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getTaskDetail membuka pembungkus data dan menyimpan ke cache`() = runTest {
        coEvery { api.getTaskDetail(1) } returns Response.success(TaskDetailResponse(task()))
        val result = repo.getTaskDetail(1)
        assertEquals(task(), result.getOrNull())
        coVerify(exactly = 1) { taskDao.insertTask(task().toEntity()) }
    }

    @Test
    fun `getTaskDetail fallback ke cache lokal saat offline`() = runTest {
        coEvery { api.getTaskDetail(1) } throws IOException("No internet")
        coEvery { taskDao.getTaskById(1) } returns task().toEntity()

        val result = repo.getTaskDetail(1)
        assertTrue(result.isSuccess)
        assertEquals(task(), result.getOrNull())
    }

    @Test
    fun `updateStatus memakai status dari server`() = runTest {
        coEvery { api.updateTaskStatus(1, any()) } returns
            Response.success(UpdateStatusResponse("ok", "sedang_memperbaiki"))
        assertEquals("sedang_memperbaiki", repo.updateStatus(1, StatusAction.START).getOrNull())
    }

    @Test
    fun `updateStatus fallback ke status yang dikirim bila server tak mengembalikan`() = runTest {
        coEvery { api.updateTaskStatus(1, any()) } returns
            Response.success(UpdateStatusResponse("ok", null))
        assertEquals("done", repo.updateStatus(1, StatusAction.FINISH).getOrNull())
    }

    @Test
    fun `updateStatus mengirim description sebagai catatan pekerjaan`() = runTest {
        val bodySlot = slot<UpdateStatusRequest>()
        coEvery { api.updateTaskStatus(1, capture(bodySlot)) } returns
            Response.success(UpdateStatusResponse("ok", "selesai"))
        repo.updateStatus(1, StatusAction.FINISH, "Ganti konektor RJ45")
        assertEquals("done", bodySlot.captured.status)
        assertEquals("Ganti konektor RJ45", bodySlot.captured.description)
    }

    @Test
    fun `updateStatus tanpa catatan tidak mengirim description`() = runTest {
        val bodySlot = slot<UpdateStatusRequest>()
        coEvery { api.updateTaskStatus(1, capture(bodySlot)) } returns
            Response.success(UpdateStatusResponse("ok", "selesai"))
        repo.updateStatus(1, StatusAction.FINISH, "   ")
        assertNull(bodySlot.captured.description)
    }

    @Test
    fun `uploadRepairPhoto sukses bila kompres & unggah berhasil`() = runTest {
        val part = MultipartBody.Part.createFormData("photo", "x.jpg", "x".toRequestBody())
        coEvery { imageCompressor.toPhotoPart(any()) } returns part
        coEvery { api.uploadRepairPhoto(1, any(), any()) } returns
            Response.success(201, PhotoUploadResponse("ok", RepairPhoto("http://x/y.jpg")))
        assertTrue(repo.uploadRepairPhoto(1, mockk<Uri>(), null).isSuccess)
    }

    @Test
    fun `uploadRepairPhoto gagal bila gambar tak bisa dibaca`() = runTest {
        coEvery { imageCompressor.toPhotoPart(any()) } returns null
        val result = repo.uploadRepairPhoto(1, mockk<Uri>(), null)
        assertTrue(result.isFailure)
    }

    @Test
    fun `uploadHousePhoto meneruskan error 422 dari backend`() = runTest {
        val part = MultipartBody.Part.createFormData("photo", "x.jpg", "x".toRequestBody())
        coEvery { imageCompressor.toPhotoPart(any()) } returns part
        val body = """{"message":"bukan laporan pelanggan, tidak ada rumah untuk difoto"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.uploadHousePhoto(1, any(), any()) } returns Response.error(422, body)
        val result = repo.uploadHousePhoto(1, mockk<Uri>(), null)
        assertTrue(result.isFailure)
        assertEquals(
            "bukan laporan pelanggan, tidak ada rumah untuk difoto",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun `error 404 diteruskan sebagai kegagalan dengan pesan`() = runTest {
        val body = """{"message":"Data tidak ditemukan"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.getTaskDetail(99) } returns Response.error(404, body)
        val result = repo.getTaskDetail(99)
        assertTrue(result.isFailure)
        assertEquals("Data tidak ditemukan", result.exceptionOrNull()?.message)
    }
}
