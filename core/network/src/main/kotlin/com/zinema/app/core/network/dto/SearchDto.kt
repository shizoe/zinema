package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Search request/response models (recovered from the decompiled client).
 *
 * ⚠️ Response field names are reconstructed; `ignoreUnknownKeys` tolerates extras,
 * but confirm `data.items` is the result list against a captured response.
 */
@Serializable
data class SearchRequestBody(
    val keyword: String,
    val page: Int = 1,
    val perPage: Int = 20,
)

@Serializable
data class SearchResultData(
    val items: List<SubjectItem> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class SearchSuggestData(
    val keywords: List<String> = emptyList(),
    val subjects: List<SubjectItem> = emptyList(),
)

@Serializable
data class HotSearchData(
    val subjects: List<SubjectItem> = emptyList(),
)
