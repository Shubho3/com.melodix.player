package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.AppUser
import com.melodix.player.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(val isLoading: Boolean = false, val error: String? = null)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Session state — null means signed out. Drives routing/session. */
    val currentUser: StateFlow<AppUser?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Called with the ID token returned by GoogleAuthClient. */
    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signInWithGoogleIdToken(idToken)
                .onSuccess { _uiState.value = AuthUiState() }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Sign-in failed") }
        }
    }

    fun onSignInError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
