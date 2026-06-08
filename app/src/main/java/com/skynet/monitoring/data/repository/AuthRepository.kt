package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.FcmTokenRequest
import com.skynet.monitoring.data.api.model.LoginRequest
import com.skynet.monitoring.data.api.model.User
import com.skynet.monitoring.data.local.UserPreferences
import com.skynet.monitoring.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AuthRepository {
    val tokenFlow: Flow<String?>
    val userFlow: Flow<User?>

    /** Login + simpan sesi ke DataStore bila sukses. */
    suspend fun login(email: String, password: String): Result<User>

    /** Kirim/perbarui FCM token ke backend. */
    suspend fun updateFcmToken(token: String): Result<Unit>

    /** Logout: hapus token server (best-effort) lalu SELALU bersihkan sesi lokal. */
    suspend fun logout(): Result<Unit>
}

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val prefs: UserPreferences,
    private val gson: Gson,
) : AuthRepository {

    override val tokenFlow: Flow<String?> = prefs.tokenFlow
    override val userFlow: Flow<User?> = prefs.userFlow

    override suspend fun login(email: String, password: String): Result<User> =
        safeApiCall(gson) { api.login(LoginRequest(email, password)) }
            .onSuccess { prefs.saveSession(it.token, it.user) }
            .map { it.user }

    override suspend fun updateFcmToken(token: String): Result<Unit> =
        safeApiCall(gson) { api.updateFcmToken(FcmTokenRequest(token)) }.map { }

    override suspend fun logout(): Result<Unit> {
        // Hapus token di server bila bisa; abaikan kegagalan (mis. offline).
        runCatching { api.logout() }
        prefs.clear()
        return Result.success(Unit)
    }
}
