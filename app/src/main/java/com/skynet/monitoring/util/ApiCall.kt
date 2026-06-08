package com.skynet.monitoring.util

import com.google.gson.Gson
import com.skynet.monitoring.data.api.model.ErrorResponse
import retrofit2.Response
import java.io.IOException

/**
 * Exception standar hasil pemanggilan API. [code] = HTTP status (null bila error jaringan),
 * dipakai ViewModel mis. mendeteksi 401 → paksa logout.
 */
class ApiException(
    override val message: String,
    val code: Int? = null,
) : Exception(message)

/**
 * Membungkus pemanggilan Retrofit menjadi [Result]. Sukses → body; gagal → [ApiException]
 * dengan pesan yang sudah di-parse dari response error backend.
 */
suspend fun <T> safeApiCall(
    gson: Gson,
    call: suspend () -> Response<T>,
): Result<T> = try {
    val response = call()
    if (response.isSuccessful) {
        response.body()?.let { Result.success(it) }
            ?: Result.failure(ApiException("Respons kosong dari server"))
    } else {
        Result.failure(ApiException(parseErrorMessage(gson, response), response.code()))
    }
} catch (e: IOException) {
    Result.failure(ApiException("Periksa koneksi internet Anda"))
} catch (e: Exception) {
    Result.failure(ApiException(e.message ?: "Terjadi kesalahan"))
}

private fun parseErrorMessage(gson: Gson, response: Response<*>): String {
    val raw = response.errorBody()?.string()
    val parsed = raw?.let {
        runCatching { gson.fromJson(it, ErrorResponse::class.java) }.getOrNull()
    }
    // 422 (validasi): ambil pesan pertama dari objek errors
    parsed?.errors?.values?.firstOrNull()?.firstOrNull()?.let { return it }
    parsed?.message?.let { if (it.isNotBlank()) return it }
    return when (response.code()) {
        401 -> "Sesi berakhir, silakan login kembali"
        403 -> "Aksi tidak diizinkan"
        404 -> "Data tidak ditemukan"
        else -> "Terjadi kesalahan (${response.code()})"
    }
}
