package com.skynet.monitoring.ui.screens.notifications

import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.repository.NotificationRepository
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.MainDispatcherRule
import com.skynet.monitoring.util.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<NotificationRepository>()

    private fun item(id: Int, read: Boolean) =
        NotificationItem(id, "Tugas Baru", "Anda ditugaskan", isRead = read, createdAt = null)

    @Test
    fun `markRead optimistic langsung menandai dibaca`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.getNotifications() } returns Result.success(listOf(item(1, false)))
        coEvery { repository.markRead(1) } returns Result.success(Unit)
        val vm = NotificationViewModel(repository)
        advanceUntilIdle()

        vm.markRead(1)
        // Update lokal terjadi sinkron, sebelum panggilan server selesai.
        val state = vm.uiState.value as UiState.Success
        assertTrue(state.data.first { it.id == 1 }.isRead)

        advanceUntilIdle()
        coVerify(exactly = 1) { repository.markRead(1) }
    }

    @Test
    fun `markRead gagal memuat ulang dari server (rollback)`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { repository.getNotifications() } returns Result.success(listOf(item(1, false)))
            coEvery { repository.markRead(1) } returns Result.failure(ApiException("Koneksi putus"))
            val vm = NotificationViewModel(repository)
            advanceUntilIdle()

            vm.markRead(1)
            advanceUntilIdle()

            // getNotifications dipanggil 2x: init + reload setelah gagal; state kembali belum-dibaca.
            coVerify(exactly = 2) { repository.getNotifications() }
            val state = vm.uiState.value as UiState.Success
            assertEquals(false, state.data.first { it.id == 1 }.isRead)
        }
}
