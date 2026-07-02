package com.zinema.app.core.domain.repository

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.model.PlaybackPosition
import kotlinx.coroutines.flow.Flow

/** Resume points + Continue Watching (blueprint T-027). */
interface PlaybackRepository {

    suspend fun savePosition(
        subjectId: String,
        contentType: ContentType,
        seasonIndex: Int,
        episodeIndex: Int,
        positionMs: Long,
        totalMs: Long,
    )

    suspend fun getPosition(subjectId: String): PlaybackPosition?

    /** Items with 2% < watched < 95% (blueprint T-027). */
    fun getContinueWatchingList(): Flow<List<Content>>
}
