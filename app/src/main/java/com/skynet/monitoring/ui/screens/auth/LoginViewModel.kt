package com.skynet.monitoring.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.skynet.monitoring.BuildConfig
import com.skynet.monitoring.data.local.UserPreferences
import com.skynet.monitoring.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    /**
     * (DEBUG saja) Alamat server yang diketik penguji. Diprefill dari nilai tersimpan;
     * disimpan saat login. Field-nya hanya tampil di build debug (lihat LoginScreen).
     */
    var serverUrl by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        if (BuildConfig.DEBUG) {
            viewModelScope.launch { serverUrl = userPreferences.getServerUrl().orEmpty() }
        }
    }

    fun onEmailChange(value: String) { email = value }
    fun onPasswordChange(value: String) { password = value }
    fun onServerUrlChange(value: String) { serverUrl = value }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email dan password wajib diisi")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            // Simpan alamat server sebelum login agar request login langsung menuju server itu.
            if (BuildConfig.DEBUG) userPreferences.saveServerUrl(serverUrl)
            authRepository.login(email.trim(), password)
                .onSuccess {
                    registerFcmToken()
                    _uiState.value = LoginUiState.Success
                }
                .onFailure {
                    _uiState.value = LoginUiState.Error(it.message ?: "Login gagal")
                }
        }
    }

    /** Ambil FCM token perangkat & kirim ke backend. Kegagalan diabaikan (tak menghalangi login). */
    private suspend fun registerFcmToken() {
        runCatching {
            val token = withContext(Dispatchers.IO) {
                Tasks.await(FirebaseMessaging.getInstance().token)
            }
            authRepository.updateFcmToken(token)
        }
    }

    /** Reset state error agar Snackbar tidak tampil berulang setelah ditampilkan. */
    fun consumeError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}
