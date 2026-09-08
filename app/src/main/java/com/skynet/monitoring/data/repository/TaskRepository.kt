package com.skynet.monitoring.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.PhotoUploadResponse
import com.skynet.monitoring.data.api.model.StatusAction
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.UpdateStatusRequest
import com.skynet.monitoring.data.local.room.dao.TaskDao
import com.skynet.monitoring.data.local.room.entity.TaskEntity
import com.skynet.monitoring.data.local.room.entity.toEntity
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.ImageCompressor
import com.skynet.monitoring.util.safeApiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject

interface TaskRepository {
    suspend fun getTasks(): Result<List<Task>>
    suspend fun getTaskHistory(): Result<List<Task>>
    suspend fun getTaskDetail(id: Int): Result<Task>

    /**
     * Update status tugas dengan [action] ([StatusAction.START] / [StatusAction.FINISH]).
     * [description] = catatan pekerjaan opsional (maks 1000 char).
     * Mengembalikan status hasil dari backend (mis. "sedang_memperbaiki").
     */
    suspend fun updateStatus(id: Int, action: StatusAction, description: String? = null): Result<String>

    /** Unggah foto bukti hasil kerja (semua kategori) dari [uri] kamera/galeri. */
    suspend fun uploadRepairPhoto(id: Int, uri: Uri, caption: String? = null): Result<Unit>

    /** Unggah foto rumah pelanggan (hanya tugas kategori `pelanggan`) dari [uri]. */
    suspend fun uploadHousePhoto(id: Int, uri: Uri, caption: String? = null): Result<Unit>
}

class TaskRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val taskDao: TaskDao,
    private val gson: Gson,
    private val imageCompressor: ImageCompressor,
) : TaskRepository {

    override suspend fun getTasks(): Result<List<Task>> {
        val networkResult = safeApiCall(gson) { api.getTasks() }.map { it.data }
        return networkResult.fold(
            onSuccess = { tasks ->
                runCatching { syncTasksToCache(tasks.map { it.toEntity() }) }
                Result.success(tasks)
            },
            onFailure = { error ->
                if (!shouldFallbackToCache(error)) return Result.failure(error)

                val cached = runCatching { taskDao.getActiveTasks().map { it.toDomain() } }.getOrNull().orEmpty()
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    override suspend fun getTaskHistory(): Result<List<Task>> {
        val networkResult = safeApiCall(gson) { api.getTasks("completed") }.map { it.data }
        return networkResult.fold(
            onSuccess = { tasks ->
                runCatching { syncHistoryToCache(tasks.map { it.toEntity() }) }
                Result.success(tasks)
            },
            onFailure = { error ->
                if (!shouldFallbackToCache(error)) return Result.failure(error)

                val cached = runCatching { taskDao.getHistoryTasks().map { it.toDomain() } }.getOrNull().orEmpty()
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    override suspend fun getTaskDetail(id: Int): Result<Task> {
        val networkResult = safeApiCall(gson) { api.getTaskDetail(id) }.map { it.data }
        return networkResult.fold(
            onSuccess = { task ->
                runCatching { taskDao.insertTask(task.toEntity()) }
                Result.success(task)
            },
            onFailure = { error ->
                if (!shouldFallbackToCache(error)) return Result.failure(error)

                val cached = runCatching { taskDao.getTaskById(id)?.toDomain() }.getOrNull()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    private suspend fun syncTasksToCache(incomingTasks: List<TaskEntity>) {
        val currentCached = taskDao.getActiveTasks().associateBy { it.id }
        val merged = incomingTasks.map { incoming ->
            val existing = currentCached[incoming.id]
            if (existing != null) {
                incoming.copy(
                    workLogs = incoming.workLogs ?: existing.workLogs,
                    housePhotos = if (incoming.housePhotos.isNotEmpty()) incoming.housePhotos else existing.housePhotos,
                    repairPhotos = if (incoming.repairPhotos.isNotEmpty()) incoming.repairPhotos else existing.repairPhotos,
                )
            } else {
                incoming
            }
        }
        taskDao.insertTasks(merged)
        if (incomingTasks.isNotEmpty()) {
            taskDao.deleteNotInActive(incomingTasks.map { it.id })
        } else {
            taskDao.clearActiveTasks()
        }
    }

    private suspend fun syncHistoryToCache(incomingTasks: List<TaskEntity>) {
        val currentCached = taskDao.getHistoryTasks().associateBy { it.id }
        val merged = incomingTasks.map { incoming ->
            val existing = currentCached[incoming.id]
            if (existing != null) {
                incoming.copy(
                    workLogs = incoming.workLogs ?: existing.workLogs,
                    housePhotos = if (incoming.housePhotos.isNotEmpty()) incoming.housePhotos else existing.housePhotos,
                    repairPhotos = if (incoming.repairPhotos.isNotEmpty()) incoming.repairPhotos else existing.repairPhotos,
                )
            } else {
                incoming
            }
        }
        taskDao.insertTasks(merged)
    }

    private fun shouldFallbackToCache(error: Throwable): Boolean {
        val apiException = error as? ApiException ?: return true
        val code = apiException.code ?: return true // null = error jaringan (IOException)
        return code >= 500 // server error 5xx bisa fallback ke cache
    }

    override suspend fun updateStatus(id: Int, action: StatusAction, description: String?): Result<String> =
        safeApiCall(gson) {
            api.updateTaskStatus(id, UpdateStatusRequest(action.apiValue, description?.takeIf { it.isNotBlank() }))
        }.map { response ->
            val newStatus = response.status ?: action.apiValue
            runCatching {
                val cached = taskDao.getTaskById(id)
                if (cached != null) {
                    taskDao.insertTask(cached.copy(status = newStatus))
                }
            }
            newStatus
        }

    override suspend fun uploadRepairPhoto(id: Int, uri: Uri, caption: String?): Result<Unit> =
        uploadPhoto(uri, caption) { part, cap -> api.uploadRepairPhoto(id, part, cap) }

    override suspend fun uploadHousePhoto(id: Int, uri: Uri, caption: String?): Result<Unit> =
        uploadPhoto(uri, caption) { part, cap -> api.uploadHousePhoto(id, part, cap) }

    /** Kompres [uri] → multipart, lalu jalankan [call]. Gagal-decode dibungkus sebagai kegagalan. */
    private suspend fun uploadPhoto(
        uri: Uri,
        caption: String?,
        call: suspend (MultipartBody.Part, RequestBody?) -> Response<PhotoUploadResponse>,
    ): Result<Unit> {
        val part = imageCompressor.toPhotoPart(uri)
            ?: return Result.failure(ApiException("Gambar tidak bisa dibaca. Coba foto/pilih ulang."))
        val captionBody = caption?.takeIf { it.isNotBlank() }
            ?.toRequestBody("text/plain".toMediaType())
        return safeApiCall(gson) { call(part, captionBody) }.map { }
    }
}
