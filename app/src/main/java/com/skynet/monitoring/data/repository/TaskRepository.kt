package com.skynet.monitoring.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.PhotoUploadResponse
import com.skynet.monitoring.data.api.model.StatusAction
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.UpdateStatusRequest
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
    private val gson: Gson,
    private val imageCompressor: ImageCompressor,
) : TaskRepository {

    override suspend fun getTasks(): Result<List<Task>> =
        safeApiCall(gson) { api.getTasks() }.map { it.data }

    override suspend fun getTaskDetail(id: Int): Result<Task> =
        safeApiCall(gson) { api.getTaskDetail(id) }.map { it.data }

    override suspend fun updateStatus(id: Int, action: StatusAction, description: String?): Result<String> =
        safeApiCall(gson) {
            api.updateTaskStatus(id, UpdateStatusRequest(action.apiValue, description?.takeIf { it.isNotBlank() }))
        }.map { it.status ?: action.apiValue }

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
