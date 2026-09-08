package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.LoginResponse
import com.skynet.monitoring.data.api.model.MessageResponse
import com.skynet.monitoring.data.api.model.User
import com.skynet.monitoring.data.local.UserPreferences
import com.skynet.monitoring.data.local.room.AppDatabase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private val api = mockk<ApiService>()
    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val db = mockk<AppDatabase>(relaxed = true)
    private val repo = AuthRepositoryImpl(api, prefs, db, Gson())

    private val user = User(id = 1, name = "Budi", email = "budi@skynet.id", role = "teknisi")

    @Test
    fun `login sukses menyimpan sesi dan mengembalikan user`() = runTest {
        coEvery { api.login(any()) } returns Response.success(LoginResponse("1|abc", user))
        val result = repo.login("budi@skynet.id", "secret")
        assertEquals(user, result.getOrNull())
        coVerify(exactly = 1) { prefs.saveSession("1|abc", user) }
    }

    @Test
    fun `login gagal tidak menyimpan sesi`() = runTest {
        val body = """{"message":"Email atau password salah"}"""
            .toResponseBody("application/json".toMediaType())
        coEvery { api.login(any()) } returns Response.error(401, body)
        val result = repo.login("x@y.z", "salah")
        assertTrue(result.isFailure)
        assertEquals("Email atau password salah", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { prefs.saveSession(any(), any()) }
    }

    @Test
    fun `updateFcmToken sukses mengembalikan Unit`() = runTest {
        coEvery { api.updateFcmToken(any()) } returns Response.success(MessageResponse("ok"))
        assertTrue(repo.updateFcmToken("token123").isSuccess)
    }

    @Test
    fun `logout selalu membersihkan sesi lokal walau server gagal`() = runTest {
        coEvery { api.logout() } throws RuntimeException("offline")
        val result = repo.logout()
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { prefs.clear() }
        verify(exactly = 1) { db.clearAllTables() }
    }
}
