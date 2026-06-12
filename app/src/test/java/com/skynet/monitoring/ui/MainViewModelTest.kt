package com.skynet.monitoring.ui

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.ui.navigation.Routes
import com.skynet.monitoring.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()

    @Test
    fun `token kosong mengarah ke LOGIN`() = runTest(mainDispatcherRule.dispatcher) {
        every { authRepository.tokenFlow } returns flowOf(null)
        val vm = MainViewModel(authRepository)
        try {
            vm.startDestination.test {
                assertNull(awaitItem())
                assertEquals(Routes.LOGIN, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `token ada mengarah ke TASKS`() = runTest(mainDispatcherRule.dispatcher) {
        every { authRepository.tokenFlow } returns flowOf("1|abc")
        val vm = MainViewModel(authRepository)
        try {
            vm.startDestination.test {
                assertNull(awaitItem())
                assertEquals(Routes.TASKS, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
