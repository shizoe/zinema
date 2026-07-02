package com.zinema.app.core.network

import com.zinema.app.core.network.dto.AccountExistsData
import com.zinema.app.core.network.dto.ApiResponse
import com.zinema.app.core.network.dto.AppConfigData
import com.zinema.app.core.network.dto.BottomTabData
import com.zinema.app.core.network.dto.CheckEmailBody
import com.zinema.app.core.network.dto.HotSearchData
import com.zinema.app.core.network.dto.LoginRequestBody
import com.zinema.app.core.network.dto.PlayInfoData
import com.zinema.app.core.network.dto.SearchRequestBody
import com.zinema.app.core.network.dto.SearchResultData
import com.zinema.app.core.network.dto.SearchSuggestData
import com.zinema.app.core.network.dto.SmsCodeResult
import com.zinema.app.core.network.dto.SubjectDetail
import com.zinema.app.core.network.dto.TabOperatingData
import com.zinema.app.core.network.dto.UserInfoData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("wefeed-mobile-bff/subject-api/bottom-tab")
    suspend fun getBottomTabs(): ApiResponse<BottomTabData>

    // --- Search (recovered from the decompiled client) ---

    @POST("wefeed-mobile-bff/subject-api/search/v2")
    suspend fun searchContent(@Body body: SearchRequestBody): ApiResponse<SearchResultData>

    @GET("wefeed-mobile-bff/subject-api/search-suggest")
    suspend fun getSearchSuggestions(
        @Query("keyword") keyword: String,
        @Query("perPage") perPage: Int = 10,
        @Query("resultMode") resultMode: Int = 0,
    ): ApiResponse<SearchSuggestData>

    @GET("wefeed-mobile-bff/subject-api/search-rank/v2")
    suspend fun getSearchRank(
        @Query("everyoneSearch") everyoneSearch: Int = 1,
    ): ApiResponse<HotSearchData>

    // --- Login (recovered from the decompiled client) ---

    @POST("wefeed-mobile-bff/user-api/login")
    suspend fun login(@Body body: LoginRequestBody): ApiResponse<UserInfoData>

    @POST("wefeed-mobile-bff/user-api/check-mail-account")
    suspend fun checkEmailExists(@Body body: CheckEmailBody): ApiResponse<AccountExistsData>

    @POST("wefeed-mobile-bff/user-api/get-sms-code")
    suspend fun getSmsCode(@Body body: LoginRequestBody): ApiResponse<SmsCodeResult>

    @POST("wefeed-mobile-bff/user-api/check-sms-code")
    suspend fun checkSmsCode(@Body body: LoginRequestBody): ApiResponse<UserInfoData>
}
