package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTabContentUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
) {
    operator fun invoke(tabId: Int, page: Int = 1): Flow<List<Content>> =
        contentRepository.getTabContent(tabId, page)
}
