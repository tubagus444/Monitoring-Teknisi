package com.skynet.monitoring.ui.screens.profile

import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.User
import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Event sekali-pakai layar Profil. */
sealed interface ProfileEvent {
    data object LoggedOut : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<ProfileEvent>() {

    val user: StateFlow<User?> = authRepository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    fun logout() {
        if (_isLoggingOut.value) return
        viewModelScope.launch {
            _isLoggingOut.value = true
            // logout() selalu membersihkan sesi lokal walau request server gagal.
            authRepository.logout()
            _isLoggingOut.value = false
            emitEvent(ProfileEvent.LoggedOut)
        }
    }
}
