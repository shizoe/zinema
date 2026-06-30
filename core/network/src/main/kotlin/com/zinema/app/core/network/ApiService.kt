package com.zinema.app.core.network

import com.zinema.app.core.network.dto.ApiResponse
import com.zinema.app.core.network.dto.AppConfigData
import com.zinema.app.core.network.dto.PlayInfoData
import com.zinema.app.core.network.dto.SubjectDetail
import com.zinema.app.core.network.dto.TabOperatingData
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the wefeed-mobile-bff API (blueprint §8.1).
 * Base URL: https://api6.aoneroom.com/
 */
interface ApiService {

    @GET("wefeed-mobile-bff/tab-operating")
    suspend fun getTabContent(
        @Query("tabId") tabId: Int,
        @Query("version") version: String,
        @Query("page") page: Int,
    ): ApiResponse<TabOperatingData>

    @GET("wefeed-mobile-bff/subject-api/get")
    suspend fun getSubjectDetail(
        @Query("subjectId") subjectId: String,
        @Query("se") seasonIndex: Int = 0,
    ): ApiResponse<SubjectDetail>

    @GET("wefeed-mobile-bff/subject-api/play-info")
    suspend fun getPlayInfo(
        @Query("subjectId") subjectId: String,
        @Query("se") seasonIndex: Int = 0,
        @Query("ep") episodeIndex: Int = 0,
    ): ApiResponse<PlayInfoData>

    @GET("wefeed-mobile-bff/app/config")
    suspend fun getAppConfig(): ApiResponse<AppConfigData>
}
