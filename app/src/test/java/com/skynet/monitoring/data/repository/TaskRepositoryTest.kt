package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.TaskDetailResponse
import com.skynet.monitoring.data.api.model.TaskListResponse
import com.skynet.monitoring.data.api.model.UpdateStatusResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TaskRepositoryTest {

    private val api = mockk<ApiService>()
    private val repo = TaskRepositoryImpl(api, Gson())

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
    fun `getTasks membuka pembungkus data`() = runTest {
        coEvery { api.getTasks() } returns Response.success(TaskListResponse(listOf(task())))
        assertEquals(listOf(task()), repo.getTasks().getOrNull())
    }

    @Test
    fun `getTaskDetail membuka pembungkus data`() = runTest {
        coEvery { api.getTaskDetail(1) } returns Response.success(TaskDetailResponse(task()))
        assertEquals(task(), repo.getTaskDetail(1).getOrNull())
    }

    @Test
    fun `updateStatus memakai status dari server`() = runTest {
        coEvery { api.updateTaskStatus(1, any()) } returns
            Response.success(UpdateStatusResponse("ok", "sedang_memperbaiki"))
        assertEquals("sedang_memperbaiki", repo.updateStatus(1, "in_progress").getOrNull())
    }

    @Test
    fun `updateStatus fallback ke status yang dikirim bila server tak mengembalikan`() = runTest {
        coEvery { api.updateTaskStatus(1, any()) } returns
            Response.success(UpdateStatusResponse("ok", null))
        assertEquals("done", repo.updateStatus(1, "done").getOrNull())
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
