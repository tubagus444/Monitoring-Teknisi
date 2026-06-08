package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.UpdateStatusRequest
import com.skynet.monitoring.util.safeApiCall
import javax.inject.Inject

interface TaskRepository {
    suspend fun getTasks(): Result<List<Task>>
    suspend fun getTaskDetail(id: Int): Result<Task>

    /**
     * Update status tugas. [status] = "in_progress" atau "done".
     * Mengembalikan status hasil dari backend (mis. "sedang_memperbaiki").
     */
    suspend fun updateStatus(id: Int, status: String): Result<String>
}

class TaskRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) : TaskRepository {

    override suspend fun getTasks(): Result<List<Task>> =
        safeApiCall(gson) { api.getTasks() }.map { it.data }

    override suspend fun getTaskDetail(id: Int): Result<Task> =
        safeApiCall(gson) { api.getTaskDetail(id) }.map { it.data }

    override suspend fun updateStatus(id: Int, status: String): Result<String> =
        safeApiCall(gson) { api.updateTaskStatus(id, UpdateStatusRequest(status)) }
            .map { it.status ?: status }
}
