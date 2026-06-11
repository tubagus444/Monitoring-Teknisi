package com.skynet.monitoring.ui.screens.working

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.ui.BaseViewModel
import com.skynet.monitoring.util.UiState
import com.skynet.monitoring.util.collectResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Event sekali-pakai layar Sedang Memperbaiki. */
sealed interface WorkingEvent {
    /** Status berhasil diubah ke done → stop GPS & kembali ke daftar tugas. */
    data object Finished : WorkingEvent
    data class ShowError(val message: String) : WorkingEvent
}

@HiltViewModel
class WorkingViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<WorkingEvent>() {

    val taskId: Int = checkNotNull(savedStateHandle["id"])

    private val _task = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val task: StateFlow<UiState<Task>> = _task.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    init {
        load()
        startTimer()
        // LocationService di-start oleh WorkingScreen saat task termuat & izin lokasi granted.
    }

    private fun load() = _task.collectResult(viewModelScope) { taskRepository.getTaskDetail(taskId) }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
    }

    /** Tombol "Tandai Selesai": kirim done, lalu picu stop GPS & kembali ke daftar tugas. */
    fun finishRepair() {
        viewModelScope.launch {
            _isFinishing.value = true
            taskRepository.updateStatus(taskId, "done")
                .onSuccess { emitEvent(WorkingEvent.Finished) }
                .onFailure { emitEvent(WorkingEvent.ShowError(it.message ?: "Gagal menyelesaikan tugas")) }
            _isFinishing.value = false
        }
    }
}
