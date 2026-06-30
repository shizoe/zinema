package com.zinema.app.navigation

import androidx.lifecycle.ViewModel
import com.zinema.app.core.domain.session.AuthState
import com.zinema.app.core.domain.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the app-wide [AuthState] to the navigation graphs (re-auth + banners). */
@HiltViewModel
class AppViewModel @Inject constructor(
    sessionManager: SessionManager,
) : ViewModel() {
    val authState: StateFlow<AuthState> = sessionManager.authState
}
