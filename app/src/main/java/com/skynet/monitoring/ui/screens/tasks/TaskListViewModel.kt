package com.skynet.monitoring.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Task>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            taskRepository.getTasks()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Gagal memuat tugas") }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            taskRepository.getTasks()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure {
                    // Pertahankan data lama bila sudah ada; tampilkan error hanya bila belum ada data.
                    if (_uiState.value !is UiState.Success) {
                        _uiState.value = UiState.Error(it.message ?: "Gagal memuat tugas")
                    }
                }
            _isRefreshing.value = false
        }
    }
}
