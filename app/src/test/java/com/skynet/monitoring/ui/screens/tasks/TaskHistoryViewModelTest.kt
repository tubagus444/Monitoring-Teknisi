package com.skynet.monitoring.ui.screens.tasks

import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.MainDispatcherRule
import com.skynet.monitoring.util.TaskSyncNotifier
import com.skynet.monitoring.util.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository = mockk<TaskRepository>()

    private fun task(id: Int = 1) =
        Task(id, 5, "selesai", "Pak Ahmad", "Jl. Mawar", "Kabel Putus", null, null, completedAt = "2026-06-01T10:00:00+07:00")

    @Test
    fun `load sukses menghasilkan Success`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskHistory() } returns Result.success(listOf(task()))
        val vm = TaskHistoryViewModel(taskRepository)
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)
    }

    @Test
    fun `load gagal menghasilkan Error`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskHistory() } returns Result.failure(ApiException("Gagal memuat"))
        val vm = TaskHistoryViewModel(taskRepository)
        advanceUntilIdle()
        assertEquals(UiState.Error("Gagal memuat"), vm.uiState.value)
    }

    @Test
    fun `refresh mempertahankan data lama saat gagal`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskHistory() } returns Result.success(listOf(task()))
        val vm = TaskHistoryViewModel(taskRepository)
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)

        coEvery { taskRepository.getTaskHistory() } returns Result.failure(ApiException("Koneksi putus"))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)
        assertEquals(false, vm.isRefreshing.value)
    }

    @Test
    fun `syncEvents memicu refresh otomatis`() = runTest(mainDispatcherRule.dispatcher) {
        val notifier = TaskSyncNotifier()
        coEvery { taskRepository.getTaskHistory() } returns Result.success(listOf(task()))
        val vm = TaskHistoryViewModel(taskRepository, notifier)
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)

        val newTask = task(id = 2)
        coEvery { taskRepository.getTaskHistory() } returns Result.success(listOf(task(), newTask))
        notifier.notifyTaskUpdate()
        advanceUntilIdle()

        assertEquals(UiState.Success(listOf(task(), newTask)), vm.uiState.value)
    }
}
