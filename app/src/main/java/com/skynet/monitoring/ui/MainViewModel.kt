package com.skynet.monitoring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Menentukan layar awal berdasarkan ada/tidaknya token di DataStore.
 * `null` = masih memuat (tampilkan loading).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val startDestination: StateFlow<String?> = authRepository.tokenFlow
        .map { token -> if (token.isNullOrBlank()) Routes.LOGIN else Routes.TASKS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
