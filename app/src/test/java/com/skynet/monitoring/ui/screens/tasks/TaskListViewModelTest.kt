package com.skynet.monitoring.ui.screens.tasks

import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.MainDispatcherRule
import com.skynet.monitoring.util.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository = mockk<TaskRepository>()

    private fun task(id: Int = 1) =
        Task(id, 5, "ditugaskan", "Pak Ahmad", "Jl. Mawar", "Kabel Putus", null, null)

    @Test
    fun `load sukses menghasilkan Success`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTasks() } returns Result.success(listOf(task()))
        val vm = TaskListViewModel(taskRepository)
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)
    }

    @Test
    fun `load gagal menghasilkan Error`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTasks() } returns Result.failure(ApiException("Gagal memuat"))
        val vm = TaskListViewModel(taskRepository)
        advanceUntilIdle()
        assertEquals(UiState.Error("Gagal memuat"), vm.uiState.value)
    }

    @Test
    fun `refresh mempertahankan data lama saat gagal`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTasks() } returns Result.success(listOf(task()))
        val vm = TaskListViewModel(taskRepository)
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)

        coEvery { taskRepository.getTasks() } returns Result.failure(ApiException("Koneksi putus"))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(UiState.Success(listOf(task())), vm.uiState.value)
        assertEquals(false, vm.isRefreshing.value)
    }
}
