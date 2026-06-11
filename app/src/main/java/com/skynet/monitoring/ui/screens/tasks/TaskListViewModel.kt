package com.skynet.monitoring.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.UiState
import com.skynet.monitoring.util.collectResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun load() = _uiState.collectResult(viewModelScope) { taskRepository.getTasks() }

    fun refresh() = _uiState.collectResult(viewModelScope, _isRefreshing) { taskRepository.getTasks() }
}
