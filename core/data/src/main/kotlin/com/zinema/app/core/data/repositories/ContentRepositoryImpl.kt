package com.zinema.app.core.data.repositories

import com.zinema.app.core.data.db.cache.ContentCacheModel
import com.zinema.app.core.data.db.cache.toCacheModel
import com.zinema.app.core.data.db.cache.toContent
import com.zinema.app.core.data.db.daos.TabCacheDao
import com.zinema.app.core.data.db.entities.CachedTabEntity
import com.zinema.app.core.data.mappers.toContentDetail
import com.zinema.app.core.data.mappers.toDomain
import com.zinema.app.core.domain.exception.StreamSecurityException
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentDetail
import com.zinema.app.core.domain.model.Episode
import com.zinema.app.core.domain.model.StreamInfo
import com.zinema.app.core.domain.repository.ContentRepository
import com.zinema.app.core.network.ApiService
import com.zinema.app.core.network.cdn.CdnValidator
import com.zinema.app.core.network.dto.SubjectItem
import com.zinema.app.core.network.dto.TabOperatingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Content browsing/detail/search/stream resolution (blueprint T-026). */
class ContentRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tabCacheDao: TabCacheDao,
) : ContentRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheSerializer = ListSerializer(ContentCacheModel.serializer())

    override fun getTabContent(tabId: Int, page: Int): Flow<List<Content>> = flow {
        val cached = tabCacheDao.getByTabId(tabId)
        val now = System.currentTimeMillis()

        // Cache hit within TTL: emit cached, then stop (blueprint T-026).
        if (cached != null && now - cached.fetchedAtMs < CACHE_TTL_MS) {
            emit(decodeCache(cached.contentJson))
            return@flow
        }

        val response = api.getTabContent(tabId = tabId, version = cached?.version ?: "", page = page)
        val data = response.data
        val contents = data
            ?.extractSubjects()
            .orEmpty()
            .distinctBy { it.subjectId }
            .filter { it.subjectId.isNotBlank() }
            .map { it.toDomain() }

        tabCacheDao.upsert(
            CachedTabEntity(
                tabId = tabId,
                contentJson = encodeCache(contents),
                fetchedAtMs = now,
                version = data?.version ?: "",
            ),
        )
        emit(contents)
    }.flowOn(Dispatchers.IO)

    override fun getContentDetail(subjectId: String): Flow<ContentDetail> = flow {
        val response = api.getSubjectDetail(subjectId = subjectId)
        val detail = response.data ?: error("No detail for subjectId=$subjectId")
        emit(detail.toContentDetail())
    }.flowOn(Dispatchers.IO)

    override fun getEpisodes(subjectId: String, seasonIndex: Int): Flow<List<Episode>> = flow {
        val response = api.getSubjectDetail(subjectId = subjectId, seasonIndex = seasonIndex)
        emit(response.data?.episodes?.map { it.toDomain() } ?: emptyList())
    }.flowOn(Dispatchers.IO)

    override suspend fun getStreamInfo(
        subjectId: String,
        seasonIndex: Int,
        episodeIndex: Int,
        quality: String,
    ): StreamInfo = withContext(Dispatchers.IO) {
        val response = api.getPlayInfo(subjectId, seasonIndex, episodeIndex)
        val data = response.data ?: error("No play-info for subjectId=$subjectId")
        val streamInfo = data.toDomain(preferredQuality = quality)
        // Never play from an unverified host (blueprint T-026).
        if (!CdnValidator.isStreamHost(streamInfo.streamUrl)) {
            throw StreamSecurityException("Stream host not allowlisted: ${streamInfo.streamUrl}")
        }
        streamInfo
    }

    override fun searchContent(query: String): Flow<List<Content>> = flow {
        // TODO: no search endpoint is defined in the blueprint (§8.1). Wire this to
        // the real API path once known; emit empty for now to honor the contract.
        emit(emptyList<Content>())
    }.flowOn(Dispatchers.IO)

    private fun TabOperatingData.extractSubjects(): List<SubjectItem> =
        items.flatMap { block ->
            block.banner?.banners.orEmpty() +
                block.customData?.items.orEmpty().mapNotNull { it.subject } +
                block.subjects
        }

    private fun encodeCache(contents: List<Content>): String =
        json.encodeToString(cacheSerializer, contents.map { it.toCacheModel() })

    private fun decodeCache(value: String): List<Content> =
        runCatching {
            json.decodeFromString(cacheSerializer, value).map { it.toContent() }
        }.getOrDefault(emptyList<Content>())

    private companion object {
        const val CACHE_TTL_MS = 2L * 60 * 60 * 1000 // 2 hours
    }
}
