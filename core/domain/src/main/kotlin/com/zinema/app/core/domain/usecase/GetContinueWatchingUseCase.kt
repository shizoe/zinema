package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContinueWatchingUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    operator fun invoke(): Flow<List<Content>> =
        playbackRepository.getContinueWatchingList()
}
