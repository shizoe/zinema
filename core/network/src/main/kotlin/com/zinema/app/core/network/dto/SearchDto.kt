package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Search request/response models. Response shape confirmed against captured
 * traffic (POST subject-api/search/v2): results are grouped into topics, and the
 * actual content hits live under `data.results[].subjects[]`.
 */
@Serializable
data class SearchRequestBody(
    val keyword: String,
    val page: Int = 1,
    val perPage: Int = 20,
)

@Serializable
data class SearchResultData(
    val results: List<SearchTopic> = emptyList(),
    val pager: SearchPager? = null,
    val tabId: String = "",
    val tabs: List<SearchTab> = emptyList(),
)

@Serializable
data class SearchTopic(
    val topicType: String = "",   // e.g. "SUBJECT", "VERTICAL_RANK"
    val title: String = "",
    val subjects: List<SubjectItem> = emptyList(),
)

@Serializable
data class SearchPager(
    val hasMore: Boolean = false,
    val page: String = "",
    val nextPage: String = "",
    val perPage: Int = 0,
    val totalCount: Int = 0,
)

@Serializable
data class SearchTab(
    val tabId: String = "",
    val name: String = "",
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
