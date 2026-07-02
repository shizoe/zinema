package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.Episode
import com.zinema.app.core.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpisodesUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
) {
    operator fun invoke(subjectId: String, seasonIndex: Int): Flow<List<Episode>> =
        contentRepository.getEpisodes(subjectId, seasonIndex)
}
