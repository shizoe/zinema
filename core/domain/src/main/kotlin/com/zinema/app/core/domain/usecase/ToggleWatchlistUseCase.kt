package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.repository.UserRepository
import javax.inject.Inject

class ToggleWatchlistUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(content: Content) =
        userRepository.toggleWatchlist(content)
}
