package com.zinema.app.core.domain.repository

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.StreamInfo
import kotlinx.coroutines.flow.Flow

/** Content browsing, detail, search, and stream resolution (blueprint T-021/T-026). */
interface ContentRepository {

    /** Tab feed. Emits cached content first when fresh (< 2h), otherwise fetches. */
    fun getTabContent(tabId: Int, page: Int): Flow<List<Content>>

    /** Full detail for one subject. */
    fun getContentDetail(subjectId: String): Flow<Content>

    /**
     * Resolves a playable stream. Always fresh (never cached). Throws
     * [com.zinema.app.core.domain.exception.StreamSecurityException] if the URL is
     * not an allowlisted stream host.
     */
    suspend fun getStreamInfo(subjectId: String, seasonIndex: Int, episodeIndex: Int): StreamInfo

    /** Search by free-text query. */
    fun searchContent(query: String): Flow<List<Content>>
}
