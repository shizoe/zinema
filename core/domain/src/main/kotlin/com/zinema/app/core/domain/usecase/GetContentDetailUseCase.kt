package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContentDetailUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
) {
    operator fun invoke(subjectId: String): Flow<Content> =
        contentRepository.getContentDetail(subjectId)
}
