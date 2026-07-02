package com.zinema.app.core.network

import com.zinema.app.core.network.dto.ApiResponse
import com.zinema.app.core.network.dto.SearchResultData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the corrected search DTOs parse the real response shape (from captured
 * `subject-api/search/v2` traffic): hits are nested under `data.results[].subjects[]`,
 * and the wrapper uses `message` (not `msg`). Unknown fields must be tolerated.
 */
class SearchDtoTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Trimmed, real-shaped response (extra subject fields kept to exercise ignoreUnknownKeys).
    private val realResponse = """
        {"code":0,"message":"","data":{
          "pager":{"hasMore":false,"nextPage":"2","page":"1","perPage":15,"totalCount":0},
          "results":[
            {"topicType":"SUBJECT","title":"","subjects":[
              {"subjectId":"8906247916759695608","subjectType":1,"title":"Avatar",
               "cover":{"url":"https://pbcdn.aoneroom.com/x.jpg","width":1965,"height":2902},
               "genre":"Action, Adventure, Fantasy","imdbRatingValue":"7.9",
               "releaseDate":"2009-12-18","description":"","hasResource":true,"durationSeconds":9720}
            ]},
            {"topicType":"VERTICAL_RANK","title":"Related Collection","subjects":[]},
            {"topicType":"SUBJECT","title":"Related Music","subjects":[
              {"subjectId":"123","subjectType":1,"title":"Song",
               "cover":{"url":"https://pbcdn.aoneroom.com/y.jpg","width":1,"height":1}}
            ]}
          ],
          "tabId":"All",
          "tabs":[{"tabId":"All","name":"All","subs":[]}]
        }}
    """.trimIndent()

    @Test
    fun parsesRealSearchResponse_andFlattensSubjects() {
        val response = json.decodeFromString<ApiResponse<SearchResultData>>(realResponse)

        assertEquals(0, response.code)
        val data = requireNotNull(response.data)
        assertEquals(3, data.results.size)

        val subjects = data.results.flatMap { it.subjects }
        assertEquals(2, subjects.size)
        assertEquals("8906247916759695608", subjects.first().subjectId)
        assertEquals("Avatar", subjects.first().title)
        assertTrue(subjects.first().cover?.url?.contains("pbcdn.aoneroom.com") == true)
    }
}
