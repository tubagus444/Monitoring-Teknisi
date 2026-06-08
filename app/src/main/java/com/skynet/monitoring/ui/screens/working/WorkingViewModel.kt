package com.skynet.monitoring.ui.screens.working

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkingViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val taskId: Int = checkNotNull(savedStateHandle["id"])

    private val _task = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val task: StateFlow<UiState<Task>> = _task.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
        startTimer()
        // TODO(Fase 7): pastikan LocationService berjalan (status sudah sedang_memperbaiki).
    }

    private fun load() {
        viewModelScope.launch {
            taskRepository.getTaskDetail(taskId)
                .onSuccess { _task.value = UiState.Success(it) }
                .onFailure { _task.value = UiState.Error(it.message ?: "Gagal memuat data") }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
    }

    /** Tombol "Tandai Selesai": kirim done, stop GPS, lalu picu kembali ke daftar tugas. */
    fun finishRepair() {
        viewModelScope.launch {
            _isFinishing.value = true
            taskRepository.updateStatus(taskId, "done")
                .onSuccess {
                    // TODO(Fase 7): stop LocationService di sini.
                    _finished.value = true
                }
                .onFailure { _error.value = it.message ?: "Gagal menyelesaikan tugas" }
            _isFinishing.value = false
        }
    }

    fun consumeError() { _error.value = null }
}
