package com.zinema.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Auth screen state holder (blueprint T-037). Guest login reads
 * [BuildConfig.GUEST_JWT]; credential login delegates to the repository. Token
 * expiry is exposed via [sessionExpiredEvents] for the nav layer to observe.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Re-auth signal (HTTP 401). The nav host collects this and routes to login. */
    val sessionExpiredEvents: Flow<Unit> = authRepository.sessionExpiredEvents

    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val token = BuildConfig.GUEST_JWT
            if (token.isBlank()) {
                _uiState.value = AuthUiState.Error("Guest access is not configured.")
                return@launch
            }
            runCatching { authRepository.loginAsGuest(token) }
                .onSuccess { _uiState.value = AuthUiState.Success(token) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign-in failed.") }
        }
    }

    fun loginWithCredentials(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching { authRepository.loginWithCredentials(email.trim(), password) }
                .onSuccess { token -> _uiState.value = AuthUiState.Success(token) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign-in failed.") }
        }
    }

    /** Clears a surfaced error back to Idle after it's shown. */
    fun consumeError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val token: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
