package com.skynet.monitoring.ui.screens.notifications

import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.NotificationRepository
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.MainDispatcherRule
import com.skynet.monitoring.util.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository = mockk<NotificationRepository>()
    private val taskRepository = mockk<TaskRepository>()

    private fun item(id: Int, read: Boolean, relatedId: Int? = null) =
        NotificationItem(id, "Tugas Baru", "Anda ditugaskan", type = "task_assigned", relatedId = relatedId, isRead = read, createdAt = null)

    private fun createVm(): NotificationViewModel =
        NotificationViewModel(notificationRepository, taskRepository)

    @Test
    fun `markRead optimistic langsung menandai dibaca`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { notificationRepository.getNotifications() } returns Result.success(listOf(item(1, false)))
        coEvery { notificationRepository.markRead(1) } returns Result.success(Unit)
        val vm = createVm()
        advanceUntilIdle()

        vm.markRead(1)
        // Update lokal terjadi sinkron, sebelum panggilan server selesai.
        val state = vm.uiState.value as UiState.Success
        assertTrue(state.data.first { it.id == 1 }.isRead)

        advanceUntilIdle()
        coVerify(exactly = 1) { notificationRepository.markRead(1) }
    }

    @Test
    fun `markRead gagal memuat ulang dari server (rollback)`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { notificationRepository.getNotifications() } returns Result.success(listOf(item(1, false)))
            coEvery { notificationRepository.markRead(1) } returns Result.failure(ApiException("Koneksi putus"))
            val vm = createVm()
            advanceUntilIdle()

            vm.markRead(1)
            advanceUntilIdle()

            // getNotifications dipanggil 2x: init + reload setelah gagal; state kembali belum-dibaca.
            coVerify(exactly = 2) { notificationRepository.getNotifications() }
            val state = vm.uiState.value as UiState.Success
            assertEquals(false, state.data.first { it.id == 1 }.isRead)
        }

    @Test
    fun `openNotification navigasi ke tugas terkait`() = runTest(mainDispatcherRule.dispatcher) {
        val notifItem = item(1, false, relatedId = 42)
        coEvery { notificationRepository.getNotifications() } returns Result.success(listOf(notifItem))
        coEvery { notificationRepository.markRead(1) } returns Result.success(Unit)

        val task = mockk<Task> {
            coEvery { id } returns 99
            coEvery { reportId } returns 42
        }
        coEvery { taskRepository.getTasks() } returns Result.success(listOf(task))

        val vm = createVm()
        advanceUntilIdle()

        vm.openNotification(1)
        advanceUntilIdle()

        // Verifikasi event navigasi ter-emit
        val event = vm.events.first()
        assertTrue(event is NotificationEvent.NavigateToTask)
        assertEquals(99, (event as NotificationEvent.NavigateToTask).taskId)
    }

    @Test
    fun `openNotification tanpa relatedId hanya tandai dibaca`() = runTest(mainDispatcherRule.dispatcher) {
        val notifItem = item(1, false, relatedId = null)
        coEvery { notificationRepository.getNotifications() } returns Result.success(listOf(notifItem))
        coEvery { notificationRepository.markRead(1) } returns Result.success(Unit)

        val vm = createVm()
        advanceUntilIdle()

        vm.openNotification(1)
        advanceUntilIdle()

        // markRead dipanggil tapi getTasks TIDAK (karena relatedId null)
        coVerify(exactly = 1) { notificationRepository.markRead(1) }
        coVerify(exactly = 0) { taskRepository.getTasks() }
    }
}
