package com.skynet.monitoring.ui.screens.working

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.MainDispatcherRule
import com.skynet.monitoring.util.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository = mockk<TaskRepository>()

    private fun handle(id: Int = 1) = SavedStateHandle(mapOf("id" to id))
    private fun task() =
        Task(1, 5, "sedang_memperbaiki", "Pak Ahmad", "Jl. Mawar", "Kabel Putus", null, null)

    // Catatan: WorkingViewModel memulai timer tak-terbatas di init; pakai runCurrent() (bukan
    // advanceUntilIdle) lalu batalkan viewModelScope di finally agar runTest bersih.

    @Test
    fun `load sukses menghasilkan Success`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
        val vm = WorkingViewModel(taskRepository, handle())
        try {
            runCurrent()
            assertEquals(UiState.Success(task()), vm.task.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `timer menambah elapsedSeconds seiring waktu`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
        val vm = WorkingViewModel(taskRepository, handle())
        try {
            runCurrent()
            advanceTimeBy(3_500)
            runCurrent()
            assertTrue("elapsed=${vm.elapsedSeconds.value}", vm.elapsedSeconds.value >= 3)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `finishRepair sukses mengirim event Finished`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
        coEvery { taskRepository.updateStatus(1, "done") } returns Result.success("selesai")
        val vm = WorkingViewModel(taskRepository, handle())
        try {
            runCurrent()
            vm.events.test {
                vm.finishRepair()
                assertEquals(WorkingEvent.Finished, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
