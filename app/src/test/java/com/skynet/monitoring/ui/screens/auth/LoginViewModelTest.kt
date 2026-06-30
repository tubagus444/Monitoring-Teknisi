package com.skynet.monitoring.ui.screens.auth

import app.cash.turbine.test
import com.skynet.monitoring.data.api.model.User
import com.skynet.monitoring.data.local.UserPreferences
import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val userPreferences = mockk<UserPreferences>(relaxed = true)

    @Test
    fun `field kosong menampilkan error tanpa memanggil repo`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = LoginViewModel(authRepository, userPreferences)
            vm.login()
            assertEquals(LoginUiState.Error("Email dan password wajib diisi"), vm.uiState.value)
            coVerify(exactly = 0) { authRepository.login(any(), any()) }
        }

    @Test
    fun `login gagal menampilkan pesan error dari backend`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { authRepository.login(any(), any()) } returns
                Result.failure(ApiException("Email atau password salah", 401))
            val vm = LoginViewModel(authRepository, userPreferences)
            vm.onEmailChange("budi@skynet.id")
            vm.onPasswordChange("salah")
            vm.login()
            advanceUntilIdle()
            assertEquals(LoginUiState.Error("Email atau password salah"), vm.uiState.value)
        }

    @Test
    fun `login sukses melewati Loading lalu Success`() =
        runTest(mainDispatcherRule.dispatcher) {
            val user = User(1, "Budi", "budi@skynet.id", "teknisi")
            coEvery { authRepository.login(any(), any()) } returns Result.success(user)
            coEvery { authRepository.updateFcmToken(any()) } returns Result.success(Unit)
            val vm = LoginViewModel(authRepository, userPreferences)
            vm.onEmailChange("budi@skynet.id")
            vm.onPasswordChange("secret")

            vm.uiState.test {
                assertEquals(LoginUiState.Idle, awaitItem())
                vm.login()
                assertEquals(LoginUiState.Loading, awaitItem())
                assertEquals(LoginUiState.Success, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { authRepository.login("budi@skynet.id", "secret") }
        }
}
