package com.skynet.monitoring.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.skynet.monitoring.data.api.model.StatusAction
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
class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository = mockk<TaskRepository>()

    private fun handle(id: Int = 1) = SavedStateHandle(mapOf("id" to id))
    private fun task() =
        Task(1, 5, "ditugaskan", "Pak Ahmad", "Jl. Mawar", "Kabel Putus", null, null)

    @Test
    fun `load sukses menghasilkan Success`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
        val vm = TaskDetailViewModel(taskRepository, handle())
        advanceUntilIdle()
        assertEquals(UiState.Success(task()), vm.uiState.value)
    }

    @Test
    fun `startRepair sukses mengirim event RepairStarted`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
            coEvery { taskRepository.updateStatus(1, StatusAction.START) } returns
                Result.success("sedang_memperbaiki")
            val vm = TaskDetailViewModel(taskRepository, handle())
            advanceUntilIdle()

            vm.events.test {
                vm.startRepair()
                assertEquals(TaskDetailEvent.RepairStarted, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startRepair gagal mengirim event ShowError`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
            coEvery { taskRepository.updateStatus(1, StatusAction.START) } returns
                Result.failure(ApiException("Transisi status tidak valid", 422))
            val vm = TaskDetailViewModel(taskRepository, handle())
            advanceUntilIdle()

            vm.events.test {
                vm.startRepair()
                assertEquals(TaskDetailEvent.ShowError("Transisi status tidak valid"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
