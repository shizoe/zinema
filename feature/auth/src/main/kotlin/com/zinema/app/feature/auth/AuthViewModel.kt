package com.zinema.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Auth state machine (blueprint T-037, extended to the real MovieBox flow).
 *
 * Enter email → check-mail-account, then branch:
 *  • exists && hasPassword → PASSWORD (user-api/login)
 *  • exists && !hasPassword → CODE     (get-sms-code → check-sms-code)
 *  • !exists                → CODE      (register via the same code path; isNewAccount)
 * Guest login reads [BuildConfig.GUEST_JWT].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Re-auth signal (HTTP 401), observed by the nav layer. */
    val sessionExpiredEvents: Flow<Unit> = authRepository.sessionExpiredEvents

    fun onEmailSubmit(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email address.") }
            return
        }
        launchGuarded {
            val status = authRepository.checkEmail(trimmed)
            if (status.exists && status.hasPassword) {
                _uiState.update {
                    it.copy(step = AuthStep.PASSWORD, email = trimmed, isNewAccount = false, isLoading = false)
                }
            } else {
                authRepository.sendEmailCode(trimmed)
                _uiState.update {
                    it.copy(step = AuthStep.CODE, email = trimmed, isNewAccount = !status.exists, isLoading = false)
                }
            }
        }
    }

    fun onPasswordSubmit(password: String) = launchGuarded {
        authRepository.loginWithCredentials(_uiState.value.email, password)
        markAuthenticated()
    }

    fun onCodeSubmit(code: String) = launchGuarded {
        authRepository.loginWithCode(_uiState.value.email, code.trim())
        markAuthenticated()
    }

    fun onResendCode() = launchGuarded {
        authRepository.sendEmailCode(_uiState.value.email)
        _uiState.update { it.copy(isLoading = false) }
    }

    fun loginAsGuest() {
        val token = BuildConfig.GUEST_JWT
        if (token.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Guest access is not configured.") }
            return
        }
        launchGuarded {
            authRepository.loginAsGuest(token)
            markAuthenticated()
        }
    }

    fun onBackToEmail() {
        _uiState.update { AuthUiState(email = it.email) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun markAuthenticated() {
        _uiState.update { it.copy(isLoading = false, authenticated = true) }
    }

    private inline fun launchGuarded(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { block() }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Something went wrong.") }
            }
        }
    }
}

enum class AuthStep { EMAIL, PASSWORD, CODE }

data class AuthUiState(
    val step: AuthStep = AuthStep.EMAIL,
    val email: String = "",
    val isNewAccount: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticated: Boolean = false,
)
