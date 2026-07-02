package com.zinema.app.core.data.repositories

import com.zinema.app.core.data.db.daos.PlaybackPositionDao
import com.zinema.app.core.data.db.entities.PlaybackPositionEntity
import com.zinema.app.core.data.mappers.toContentStub
import com.zinema.app.core.data.mappers.toDomain
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.model.PlaybackPosition
import com.zinema.app.core.domain.repository.PlaybackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/** Resume points + Continue Watching (blueprint T-027). */
class PlaybackRepositoryImpl @Inject constructor(
    private val playbackPositionDao: PlaybackPositionDao,
) : PlaybackRepository {

    override suspend fun savePosition(
        subjectId: String,
        contentType: ContentType,
        seasonIndex: Int,
        episodeIndex: Int,
        positionMs: Long,
        totalMs: Long,
    ) {
        playbackPositionDao.upsert(
            PlaybackPositionEntity(
                subjectId = subjectId,
                contentType = contentType.name,
                seasonIndex = seasonIndex,
                episodeIndex = episodeIndex,
                positionMs = positionMs,
                totalDurationMs = totalMs,
                updatedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun getPosition(subjectId: String): PlaybackPosition? =
        playbackPositionDao.getBySubjectId(subjectId)?.toDomain()

    override fun getContinueWatchingList(): Flow<List<Content>> = flow {
        val rows = playbackPositionDao.getAllBetweenCompletion(MIN_PCT, MAX_PCT)
        emit(rows.map { it.toContentStub() })
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val MIN_PCT = 0.02f // 2%
        const val MAX_PCT = 0.95f // 95%
    }
}
