package com.skynet.monitoring.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.skynet.monitoring.data.api.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

/**
 * Penyimpanan token + data user via DataStore. Hanya 2 key (token & user JSON).
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER = stringPreferencesKey("user_json")
    }

    /** Token reaktif. null → arahkan ke layar Login. */
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }

    val userFlow: Flow<User?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER]?.let { json ->
            runCatching { gson.fromJson(json, User::class.java) }.getOrNull()
        }
    }

    /** Pembacaan token sekali jalan (dipakai AuthInterceptor). */
    suspend fun getToken(): String? = context.dataStore.data.first()[Keys.TOKEN]

    suspend fun saveSession(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER] = gson.toJson(user)
        }
    }

    /** Hapus token + user (dipanggil saat logout / token kadaluarsa). */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
