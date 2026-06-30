package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchContentUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
) {
    operator fun invoke(query: String): Flow<List<Content>> =
        contentRepository.searchContent(query)
}
