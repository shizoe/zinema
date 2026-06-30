package com.zinema.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.model.UserProfile
import com.zinema.app.core.domain.repository.ProfileRepository
import com.zinema.app.core.domain.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [ProfileSelectorScreen] with persisted profiles + active-profile selection. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionState: SessionState,
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = profileRepository.getProfiles()
            _profiles.value = if (existing.isNotEmpty()) {
                existing
            } else {
                // First launch — seed a default profile.
                profileRepository.addProfile("Me", avatarIndex = 0, isKids = false, pin = null)
                profileRepository.getProfiles()
            }
        }
    }

    fun selectProfile(profile: UserProfile) {
        viewModelScope.launch {
            profileRepository.setActiveProfileId(profile.id)
            sessionState.isKidsProfile = profile.isKidsProfile
        }
    }

    fun addProfile(displayName: String, avatarIndex: Int, isKids: Boolean, pin: String?) {
        viewModelScope.launch {
            profileRepository.addProfile(displayName, avatarIndex, isKids, pin)
            _profiles.value = profileRepository.getProfiles()
        }
    }

    fun verifyPin(profile: UserProfile, enteredPin: String): Boolean =
        profileRepository.verifyPin(profile, enteredPin)
}
