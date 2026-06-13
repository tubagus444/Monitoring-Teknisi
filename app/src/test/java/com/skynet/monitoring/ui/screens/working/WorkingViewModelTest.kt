package com.skynet.monitoring.ui.screens.working

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.skynet.monitoring.data.api.model.StatusAction
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.WorkLog
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.DateUtils
import com.skynet.monitoring.util.MainDispatcherRule
import com.skynet.monitoring.util.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.cancel
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
    // Perhitungan detik/tick durasi diuji terpisah & deterministik di WorkDurationTest.

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
    fun `elapsedSeconds di-seed dari work log sedang_memperbaiki`() =
        runTest(mainDispatcherRule.dispatcher) {
            // Anchor 5 dtk lalu (relatif jam sistem yang sama dipakai timer) → emisi pertama ~5.
            val startedAt = DateUtils.toIso8601(System.currentTimeMillis() - 5_000)
            val seeded = task().copy(
                workLogs = listOf(WorkLog("sedang_memperbaiki", "Budi", null, startedAt)),
            )
            coEvery { taskRepository.getTaskDetail(1) } returns Result.success(seeded)
            val vm = WorkingViewModel(taskRepository, handle())
            try {
                runCurrent()
                assertTrue("elapsed=${vm.elapsedSeconds.value}", vm.elapsedSeconds.value in 4..8)
            } finally {
                vm.viewModelScope.cancel()
            }
        }

    @Test
    fun `elapsedSeconds mulai dari nol bila tak ada work log`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
            val vm = WorkingViewModel(taskRepository, handle())
            try {
                runCurrent()
                assertEquals(0L, vm.elapsedSeconds.value)
            } finally {
                vm.viewModelScope.cancel()
            }
        }

    @Test
    fun `finishRepair sukses mengirim event Finished`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { taskRepository.getTaskDetail(1) } returns Result.success(task())
        coEvery { taskRepository.updateStatus(1, StatusAction.FINISH) } returns Result.success("selesai")
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
