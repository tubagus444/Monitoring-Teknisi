package com.skynet.monitoring.ui.screens.profile

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.skynet.monitoring.data.api.model.User
import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `logout membersihkan sesi lalu mengirim event LoggedOut`() =
        runTest(mainDispatcherRule.dispatcher) {
            every { authRepository.userFlow } returns flowOf(User(1, "Budi", "b@s.id", "teknisi"))
            coEvery { authRepository.logout() } returns Result.success(Unit)
            val vm = ProfileViewModel(authRepository)
            try {
                vm.events.test {
                    vm.logout()
                    assertEquals(ProfileEvent.LoggedOut, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
                coVerify(exactly = 1) { authRepository.logout() }
            } finally {
                vm.viewModelScope.cancel()
            }
        }

    @Test
    fun `logout tetap mengirim LoggedOut walau request server gagal`() =
        runTest(mainDispatcherRule.dispatcher) {
            every { authRepository.userFlow } returns flowOf()
            coEvery { authRepository.logout() } returns Result.failure(ApiException("offline"))
            val vm = ProfileViewModel(authRepository)
            try {
                vm.events.test {
                    vm.logout()
                    assertEquals(ProfileEvent.LoggedOut, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                vm.viewModelScope.cancel()
            }
        }
}
