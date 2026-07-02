package com.zinema.app.core.domain.usecase

import com.zinema.app.core.domain.model.ContentTab
import com.zinema.app.core.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContentTabsUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
) {
    operator fun invoke(): Flow<List<ContentTab>> = contentRepository.getContentTabs()
}
