package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.repository.PlaybackRepository
import javax.inject.Inject

class SavePlaybackPositionUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    suspend operator fun invoke(
        subjectId: String,
        contentType: ContentType,
        seasonIndex: Int,
        episodeIndex: Int,
        positionMs: Long,
        totalMs: Long,
    ) = playbackRepository.savePosition(
        subjectId = subjectId,
        contentType = contentType,
        seasonIndex = seasonIndex,
        episodeIndex = episodeIndex,
        positionMs = positionMs,
        totalMs = totalMs,
    )
}
