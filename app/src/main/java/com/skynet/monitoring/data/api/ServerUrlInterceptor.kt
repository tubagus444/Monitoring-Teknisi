package com.skynet.monitoring.data.api

import com.skynet.monitoring.data.local.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * (DEBUG saja) Mengarahkan ulang setiap request ke alamat server yang diisi penguji di
 * layar Login (tersimpan di DataStore). Hanya **skema/host/port** yang ditimpa — path
 * `/api/...` dari [com.skynet.monitoring.BuildConfig.BASE_URL] tetap utuh.
 *
 * Bila override kosong/invalid, request jalan apa adanya (pakai BASE_URL bawaan). Interceptor
 * ini hanya ditambahkan ke OkHttp pada build **debug** (lihat NetworkModule), jadi build
 * release selalu memakai BASE_URL resmi tanpa bisa ditimpa.
 *
 * Token dibaca blocking dari DataStore — konsisten dgn [AuthInterceptor], acceptable di skala ini.
 */
class ServerUrlInterceptor @Inject constructor(
    private val userPreferences: UserPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val override = runBlocking { userPreferences.getServerUrl() }
        val parsed = normalizeServerUrl(override)
            ?: return chain.proceed(chain.request())

        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .scheme(parsed.scheme)
            .host(parsed.host)
            .port(parsed.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}

/**
 * Normalkan input penguji jadi [HttpUrl]; null bila kosong/invalid.
 * Menerima "192.168.0.105:8000", "skynet-monitoring.tech", atau "https://api.skynet.id".
 * Tanpa skema:
 * - IP lokal/LAN/localhost -> default `http://`
 * - Domain publik -> default `https://`
 */
fun normalizeServerUrl(input: String?): HttpUrl? {
    val trimmed = input?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        val isLocal = trimmed.startsWith("192.168.") ||
            trimmed.startsWith("10.") ||
            trimmed.startsWith("172.") ||
            trimmed.startsWith("127.0.0.1") ||
            trimmed.startsWith("localhost")
        if (isLocal) "http://$trimmed" else "https://$trimmed"
    }
    return withScheme.toHttpUrlOrNull()
}
