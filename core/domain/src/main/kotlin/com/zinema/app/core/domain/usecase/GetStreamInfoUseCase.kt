package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.StreamInfo
import com.zinema.app.core.domain.repository.ContentRepository
import javax.inject.Inject

class GetStreamInfoUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
) {
    suspend operator fun invoke(
        subjectId: String,
        seasonIndex: Int = 0,
        episodeIndex: Int = 0,
        quality: String = "1080",
    ): StreamInfo = contentRepository.getStreamInfo(subjectId, seasonIndex, episodeIndex, quality)
}
