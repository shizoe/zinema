# AI Agent Engineering Blueprint
## Project Zinema — Android Streaming Client (Netflix-Style)

> **Purpose:** This document is the direct build instruction set for an AI coding agent. Every decision is pre-made. Every library is pinned. Every class name and package path is defined. Do not deviate from the specifications herein without explicit instruction. Read the full document before writing a single line of code.

---

| Field | Value |
|---|---|
| **Blueprint Version** | 1.0 |
| **PRD Source** | `PRD.md` v1.0 |
| **App Style Reference** | Netflix Android app |
| **Last Revised** | 2026-06-29 |

---

## Table of Contents

1. [Resolved Decisions](#1-resolved-decisions)
2. [Tech Stack Manifest](#2-tech-stack-manifest)
3. [Gradle Module Map](#3-gradle-module-map)
4. [Package & Directory Structure](#4-package--directory-structure)
5. [API Contract — Kotlin Data Classes](#5-api-contract--kotlin-data-classes)
6. [Domain Models](#6-domain-models)
7. [Room Database Schema](#7-room-database-schema)
8. [Network Layer Specification](#8-network-layer-specification)
9. [UI Architecture & Navigation](#9-ui-architecture--navigation)
10. [Theme & Design System](#10-theme--design-system)
11. [Atomic Task List](#11-atomic-task-list)

---

## 1. Resolved Decisions

All open questions from PRD.md §9 are resolved here. The agent must use these decisions without question.

| OQ | Decision |
|---|---|
| **OQ-01 Clock skew** | Server tolerance assumed 5 minutes. On `407` response, fetch time from `/wefeed-mobile-bff/app/config` and store offset as `serverTimeOffsetMs = serverTs - System.currentTimeMillis()`. Apply offset to all subsequent `System.currentTimeMillis()` calls in the signing interceptor. |
| **OQ-02 Device ID validation** | Use `SHA-256(ANDROID_ID)` as device_id. Stable per install, not per boot. Store the hashed value in `EncryptedSharedPreferences` on first launch and reuse. |
| **OQ-03 Key rotation** | The app checks `data.signKeyVersion` from `/wefeed-mobile-bff/app/config`. If version ≠ 2, log a warning and continue using version 2. Key rotation is a backend-forced update; no in-app mechanism needed for v1. |
| **OQ-04 play-info headers** | Always send `x-play-mode: 2` and `x-content-mode: 0` (or `1` if Kids profile). No premium-lock logic in v1; the server returns what it returns. |
| **OQ-05 Rate limits** | Implement client-side throttle: max 1 `tab-operating` request per tab per 500ms using a `conflate()`d flow. No global rate limiter needed. |
| **OQ-06 tabId=27 (Live)** | Suppress from navigation entirely in v1. Do not show a "Coming Soon" banner. Tab simply does not appear. Add a `SUPPRESSED_TAB_IDS = setOf(27)` constant to the tab config. |
| **OQ-07 Continue Watching ID** | Use `subjectId` (platform's own identifier, string) as the stable key. Never use PocketBase record ID. |
| **OQ-08 Geo-blocking** | When the API returns an empty `subjects` list or a `data.code` indicating unavailability, display the empty state screen with message "Content not available in your region." Do not hide the tab itself. |
| **Mobile vs TV APK** | Single APK. At `MainActivity` startup, check `UiModeManager.getCurrentModeType() == UI_MODE_TYPE_TELEVISION`. Navigate to the TV-specific `NavHost` or mobile `NavHost` accordingly. |
| **Compose vs Views for TV** | Use `androidx.tv:tv-material:1.0.0` (Compose TV) for both platforms. No Leanback Fragments. Full Compose across the app. |
| **State management** | MVVM + UDF. All ViewModels expose `StateFlow<UiState>`. All UI events flow up as sealed `UiEvent` classes. No `LiveData`. |
| **Dependency Injection** | Hilt throughout. Every ViewModel uses `@HiltViewModel`. Every Repository is `@Singleton`. |
| **Serialization** | Kotlinx Serialization (not Moshi, not Gson). All API models annotated with `@Serializable`. |

---

## 2. Tech Stack Manifest

### 2.1 Core Versions

```toml
# gradle/libs.versions.toml

[versions]
kotlin                  = "2.0.21"
agp                     = "8.7.3"
compose-bom             = "2024.12.01"
compose-tv              = "1.0.0"
media3                  = "1.4.1"
hilt                    = "2.52"
room                    = "2.6.1"
retrofit                = "2.11.0"
okhttp                  = "4.12.0"
coil                    = "2.7.0"
kotlinx-serialization   = "1.7.3"
kotlinx-coroutines      = "1.9.0"
navigation-compose      = "2.8.4"
security-crypto         = "1.1.0-alpha06"
datastore               = "1.1.1"
splash-screen           = "1.0.1"
accompanist-systemuicontroller = "0.36.0"
```

### 2.2 Full Dependency Declaration

```toml
[libraries]
# --- Compose ---
compose-bom                     = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui                      = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling              = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview      = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3               = { group = "androidx.compose.material3", name = "material3" }
compose-runtime                 = { group = "androidx.compose.runtime", name = "runtime" }
compose-foundation              = { group = "androidx.compose.foundation", name = "foundation" }
compose-animation               = { group = "androidx.compose.animation", name = "animation" }
compose-activity                = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }
compose-viewmodel               = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.7" }
compose-lifecycle-runtime       = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }

# --- Compose TV ---
tv-material                     = { group = "androidx.tv", name = "tv-material", version.ref = "compose-tv" }
tv-foundation                   = { group = "androidx.tv", name = "tv-foundation", version.ref = "compose-tv" }

# --- Navigation ---
navigation-compose              = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# --- Hilt ---
hilt-android                    = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler                   = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose         = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# --- Room ---
room-runtime                    = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx                        = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler                   = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# --- Network ---
retrofit-core                   = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization  = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version = "1.0.0" }
okhttp-core                     = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging                  = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json      = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# --- Coil ---
coil-compose                    = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
coil-video                      = { group = "io.coil-kt", name = "coil-video", version.ref = "coil" }

# --- Media3 (ExoPlayer) ---
media3-exoplayer                = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-exoplayer-dash           = { group = "androidx.media3", name = "media3-exoplayer-dash", version.ref = "media3" }
media3-exoplayer-hls            = { group = "androidx.media3", name = "media3-exoplayer-hls", version.ref = "media3" }
media3-ui                       = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
media3-session                  = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-datasource-okhttp        = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "media3" }

# --- Security ---
security-crypto                 = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
datastore-preferences           = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# --- Coroutines ---
coroutines-android              = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }

# --- Splash Screen ---
splashscreen                    = { group = "androidx.core", name = "core-splashscreen", version.ref = "splash-screen" }

# --- Accompanist ---
accompanist-systemuicontroller  = { group = "com.google.accompanist", name = "accompanist-systemuicontroller", version.ref = "accompanist-systemuicontroller" }

# --- Firebase ---
firebase-bom                    = { group = "com.google.firebase", name = "firebase-bom", version = "33.7.0" }
firebase-analytics              = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics            = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }

[plugins]
android-application     = { id = "com.android.application", version.ref = "agp" }
android-library         = { id = "com.android.library", version.ref = "agp" }
kotlin-android          = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt             = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
kotlin-serialization    = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt                    = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services         = { id = "com.google.gms.google-services", version = "4.4.2" }
crashlytics             = { id = "com.google.firebase.crashlytics", version = "3.0.2" }
ksp                     = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
compose-compiler        = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

---

## 3. Gradle Module Map

```
root/
├── app/                          ← Single application module (orchestrator)
├── core/
│   ├── network/                  ← OkHttp, Retrofit, signing interceptor, API service interfaces
│   ├── data/                     ← Repository implementations, Room DB, DataStore
│   ├── domain/                   ← Use cases, repository interfaces, domain models (pure Kotlin)
│   ├── ui/                       ← Shared composables, theme, typography, colors
│   └── security/                 ← JWT storage, EncryptedSharedPreferences wrapper
├── feature/
│   ├── auth/                     ← Login screen, profile selector, guest mode
│   ├── home/                     ← Browse feed, tab navigation, rail components
│   ├── detail/                   ← Content detail screen, episode list
│   ├── player/                   ← ExoPlayer integration, player controls, PiP
│   ├── search/                   ← Search screen, recent searches
│   └── shorttv/                  ← Vertical swipe feed (mobile only)
```

### 3.1 Module Dependency Graph

```
app
 ├── feature:auth
 ├── feature:home
 ├── feature:detail
 ├── feature:player
 ├── feature:search
 └── feature:shorttv

feature:* → core:domain
feature:* → core:ui

core:data   → core:domain
core:data   → core:network
core:data   → core:security

core:network → core:security (for token)
core:ui      → (no internal deps)
core:domain  → (no internal deps — pure Kotlin)
core:security → (no internal deps)
```

### 3.2 App Module `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.zinema.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.zinema.app"
        // App display name is set in res/values/strings.xml as "Zinema"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

---

## 4. Package & Directory Structure

All Kotlin source lives under `com.zinema.app`. Full tree:

```
com.zinema.app/
│
├── MainActivity.kt                        ← Single activity, platform detector
│
├── core/
│   ├── network/
│   │   ├── ApiService.kt                  ← Retrofit interface definitions
│   │   ├── NetworkModule.kt               ← Hilt module: OkHttp, Retrofit, Coil
│   │   ├── interceptors/
│   │   │   ├── SigningInterceptor.kt       ← x-tr-signature HMAC-MD5
│   │   │   ├── AuthInterceptor.kt         ← Bearer JWT injection
│   │   │   ├── ClientInfoInterceptor.kt   ← x-client-info + session headers
│   │   │   └── LogScrubInterceptor.kt     ← Strips JWT from logs
│   │   ├── cdn/
│   │   │   ├── CdnValidator.kt            ← Hostname allowlist enforcement
│   │   │   └── CloudFrontCookieJar.kt     ← play_auth cookie injection for CDN
│   │   └── dto/                           ← API response data classes (@Serializable)
│   │       ├── TabOperatingResponse.kt
│   │       ├── SubjectDetailResponse.kt
│   │       ├── PlayInfoResponse.kt
│   │       └── AppConfigResponse.kt
│   │
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt             ← Room database
│   │   │   ├── entities/
│   │   │   │   ├── CachedTabEntity.kt
│   │   │   │   ├── PlaybackPositionEntity.kt
│   │   │   │   ├── WatchlistEntity.kt
│   │   │   │   └── RecentSearchEntity.kt
│   │   │   └── daos/
│   │   │       ├── TabCacheDao.kt
│   │   │       ├── PlaybackPositionDao.kt
│   │   │       ├── WatchlistDao.kt
│   │   │       └── RecentSearchDao.kt
│   │   ├── repositories/
│   │   │   ├── ContentRepositoryImpl.kt
│   │   │   ├── PlaybackRepositoryImpl.kt
│   │   │   └── UserRepositoryImpl.kt
│   │   └── DataModule.kt                  ← Hilt module: DB, DAOs, Repos
│   │
│   ├── domain/
│   │   ├── model/                         ← Pure Kotlin domain models
│   │   │   ├── Content.kt                 ← Movie/Show/Short/Sports
│   │   │   ├── Episode.kt
│   │   │   ├── StreamInfo.kt
│   │   │   ├── ContentTab.kt
│   │   │   └── UserProfile.kt
│   │   ├── repository/                    ← Interfaces only
│   │   │   ├── ContentRepository.kt
│   │   │   ├── PlaybackRepository.kt
│   │   │   └── UserRepository.kt
│   │   └── usecase/
│   │       ├── GetTabContentUseCase.kt
│   │       ├── GetContentDetailUseCase.kt
│   │       ├── GetStreamInfoUseCase.kt
│   │       ├── GetContinueWatchingUseCase.kt
│   │       ├── SavePlaybackPositionUseCase.kt
│   │       ├── SearchContentUseCase.kt
│   │       └── ToggleWatchlistUseCase.kt
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── ZinemaTheme.kt
│   │   │   ├── Color.kt
│   │   │   ├── Type.kt
│   │   │   └── Shape.kt
│   │   ├── components/
│   │   │   ├── ContentCard.kt             ← 2:3 poster card
│   │   │   ├── WideContentCard.kt         ← 16:9 thumbnail card
│   │   │   ├── ShortCard.kt               ← 9:16 vertical card
│   │   │   ├── ContentRail.kt             ← Horizontal scroll row
│   │   │   ├── HeroBanner.kt              ← Auto-advancing carousel
│   │   │   ├── GenreChip.kt
│   │   │   ├── RatingBadge.kt
│   │   │   ├── ShimmerRail.kt             ← Loading skeleton
│   │   │   ├── ErrorBanner.kt
│   │   │   └── OfflineBanner.kt
│   │   └── util/
│   │       ├── ModifierExtensions.kt
│   │       └── ImageUrlValidator.kt       ← Coil interceptor hook
│   │
│   └── security/
│       ├── TokenStorage.kt                ← EncryptedSharedPreferences wrapper
│       ├── DeviceIdProvider.kt            ← SHA-256(ANDROID_ID)
│       └── SecurityModule.kt             ← Hilt module
│
├── feature/
│   ├── auth/
│   │   ├── AuthViewModel.kt
│   │   ├── LoginScreen.kt
│   │   └── ProfileSelectorScreen.kt
│   │
│   ├── home/
│   │   ├── HomeViewModel.kt
│   │   ├── HomeScreen.kt                  ← Mobile browse feed
│   │   ├── TvHomeScreen.kt                ← TV side-nav + rails
│   │   └── TabContentViewModel.kt
│   │
│   ├── detail/
│   │   ├── DetailViewModel.kt
│   │   ├── DetailScreen.kt
│   │   ├── EpisodeListSection.kt
│   │   └── TrailerPreview.kt
│   │
│   ├── player/
│   │   ├── PlayerViewModel.kt
│   │   ├── PlayerScreen.kt
│   │   ├── TvPlayerScreen.kt
│   │   ├── PlayerControls.kt
│   │   ├── TvPlayerControls.kt
│   │   ├── SubtitleTrackSelector.kt
│   │   ├── QualitySelector.kt
│   │   └── PipManager.kt
│   │
│   ├── search/
│   │   ├── SearchViewModel.kt
│   │   └── SearchScreen.kt
│   │
│   └── shorttv/
│       ├── ShortTvViewModel.kt
│       └── ShortTvScreen.kt
│
└── navigation/
    ├── AppNavigation.kt                   ← Mobile NavHost
    ├── TvNavigation.kt                    ← TV NavHost
    └── Screen.kt                          ← Sealed class of routes
```

---

## 5. API Contract — Kotlin Data Classes

All classes in `core/network/dto/`. All annotated with `@Serializable`. Unknown keys are tolerated using `ignoreUnknownKeys = true` on the Kotlinx JSON instance.

### 5.1 Common API Wrapper

```kotlin
// core/network/dto/ApiWrapper.kt
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null
)
```

### 5.2 Tab Operating Response

```kotlin
// core/network/dto/TabOperatingResponse.kt
@Serializable
data class TabOperatingData(
    val version: String = "",
    val items: List<ContentBlock> = emptyList(),
    val hasMore: Boolean = false
)

@Serializable
data class ContentBlock(
    val type: String = "",               // BANNER | SUBJECTS_MOVIE | CUSTOM | SPORT_LIVE | APPOINTMENT_LIST
    val banner: BannerBlock? = null,
    val subjects: List<SubjectItem> = emptyList(),
    val customData: CustomBlock? = null
)

@Serializable
data class BannerBlock(
    val banners: List<SubjectItem> = emptyList()
)

@Serializable
data class CustomBlock(
    val items: List<CustomItem> = emptyList()
)

@Serializable
data class CustomItem(
    val subject: SubjectItem? = null
)

@Serializable
data class SubjectItem(
    val subjectId: String = "",
    val title: String = "",
    val subjectType: Int = 1,            // 1=Movie, 2=Anime, 5=TV, 7=Short, 9=Sports
    val cover: CoverImage? = null,
    val preVideoCover: CoverImage? = null,
    val imdbRatingValue: String? = null,
    val genre: String? = null,           // comma-separated: "Action, Comedy"
    val genres: List<String>? = null,
    val releaseDate: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val totalEpisode: Int? = null,
    val totalSeason: Int? = null,
    val averageHueLight: String? = null,
    val averageHueDark: String? = null
)

@Serializable
data class CoverImage(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0
)
```

### 5.3 Subject Detail Response

```kotlin
// core/network/dto/SubjectDetailResponse.kt
@Serializable
data class SubjectDetail(
    val subjectId: String = "",
    val title: String = "",
    val subjectType: Int = 1,
    val cover: CoverImage? = null,
    val preVideoCover: CoverImage? = null,
    val description: String? = null,
    val genre: String? = null,
    val genres: List<String>? = null,
    val releaseDate: String? = null,
    val year: Int? = null,
    val imdbRatingValue: String? = null,
    val totalEpisode: Int? = null,
    val totalSeason: Int? = null,
    val seasons: List<SeasonInfo>? = null,
    val episodes: List<EpisodeInfo>? = null,
    val trailerUrl: String? = null,
    val relatedSubjects: List<SubjectItem>? = null
)

@Serializable
data class SeasonInfo(
    val seasonIndex: Int = 1,
    val seasonTitle: String = "",
    val totalEpisode: Int = 0
)

@Serializable
data class EpisodeInfo(
    val ep: Int = 0,
    val se: Int = 1,
    val title: String = "",
    val cover: CoverImage? = null,
    val durationMs: Long? = null
)
```

### 5.4 Play Info Response

```kotlin
// core/network/dto/PlayInfoResponse.kt
@Serializable
data class PlayInfoData(
    val streams: List<StreamDto> = emptyList(),
    val subtitles: List<SubtitleDto>? = null,
    val vid: String = "",
    val expiresAt: String? = null
)

@Serializable
data class StreamDto(
    val resolutions: String = "",        // "1080", "720", "480", "360"
    val url: String = "",
    val signCookie: String = "",         // CloudFront-Policy=...;CloudFront-Signature=...;CloudFront-Key-Pair-Id=...;
    val streamType: String = "dash",     // "dash" | "hls" | "mp4"
    val bandwidth: Long? = null
)

@Serializable
data class SubtitleDto(
    val language: String = "",
    val languageCode: String = "",
    val url: String = "",
    val format: String = "vtt"           // "vtt" | "srt" | "ttml"
)
```

### 5.5 App Config Response

```kotlin
// core/network/dto/AppConfigResponse.kt
@Serializable
data class AppConfigData(
    val serverTimestamp: Long = 0L,
    val signKeyVersion: Int = 2,
    val tabs: List<TabConfig>? = null
)

@Serializable
data class TabConfig(
    val tabId: Int = 0,
    val tabName: String = "",
    val tabType: String = "API",         // "API" | "H5"
    val h5Url: String? = null,
    val sortOrder: Int = 0
)
```

---

## 6. Domain Models

Pure Kotlin. No Android imports. No Serialization annotations. Lives in `core:domain`.

```kotlin
// core/domain/model/Content.kt
data class Content(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val genres: List<String>,
    val year: Int?,
    val rating: String?,
    val type: ContentType,
    val totalSeasons: Int,
    val totalEpisodes: Int,
    val trailerUrl: String?,
    val placeholderColor: Long      // parsed from averageHueDark
)

enum class ContentType {
    MOVIE, TV, ANIME, SHORT, SPORTS
}

// core/domain/model/Episode.kt
data class Episode(
    val seasonIndex: Int,
    val episodeIndex: Int,
    val title: String,
    val thumbnailUrl: String,
    val durationMs: Long
)

// core/domain/model/StreamInfo.kt
data class StreamInfo(
    val streamUrl: String,
    val quality: String,
    val streamProtocol: StreamProtocol,
    val cloudFrontPolicy: String,
    val cloudFrontSignature: String,
    val cloudFrontKeyPairId: String,
    val subtitles: List<SubtitleTrack>,
    val vid: String,
    val expiresAt: String?
)

enum class StreamProtocol { DASH, HLS, PROGRESSIVE_MP4 }

data class SubtitleTrack(
    val language: String,
    val languageCode: String,
    val url: String,
    val format: String
)

// core/domain/model/ContentTab.kt
data class ContentTab(
    val tabId: Int,
    val displayName: String,
    val keyword: String,
    val isVisible: Boolean
)

// core/domain/model/UserProfile.kt
data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarIndex: Int,
    val isKidsProfile: Boolean,
    val pin: String?
)
```

### 6.1 Domain Mappers

Create `core/data/mappers/ContentMapper.kt`:

```kotlin
fun SubjectItem.toDomain(): Content = Content(
    id = subjectId,
    title = title,
    description = description ?: "",
    posterUrl = cover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    backdropUrl = preVideoCover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    genres = genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        ?: genres ?: emptyList(),
    year = year,
    rating = imdbRatingValue?.takeIf { it != "0" && it != "0.0" },
    type = when (subjectType) {
        1 -> ContentType.MOVIE
        2 -> ContentType.ANIME
        5 -> ContentType.TV
        7 -> ContentType.SHORT
        9 -> ContentType.SPORTS
        else -> ContentType.MOVIE
    },
    totalSeasons = totalSeason ?: 0,
    totalEpisodes = totalEpisode ?: 0,
    trailerUrl = null,
    placeholderColor = averageHueDark?.parseHexColor() ?: 0xFF1A1A2E
)

fun PlayInfoData.toDomain(preferredQuality: String = "1080"): StreamInfo {
    val stream = streams.firstOrNull { it.resolutions == preferredQuality }
        ?: streams.lastOrNull()
        ?: throw IllegalStateException("No streams available")
    val cookies = stream.signCookie.parseCloudfrontCookies()
    return StreamInfo(
        streamUrl = stream.url,
        quality = stream.resolutions,
        streamProtocol = when {
            stream.url.endsWith(".mpd") -> StreamProtocol.DASH
            stream.url.endsWith(".m3u8") -> StreamProtocol.HLS
            else -> StreamProtocol.PROGRESSIVE_MP4
        },
        cloudFrontPolicy = cookies["CloudFront-Policy"] ?: "",
        cloudFrontSignature = cookies["CloudFront-Signature"] ?: "",
        cloudFrontKeyPairId = cookies["CloudFront-Key-Pair-Id"] ?: "",
        subtitles = subtitles?.map { it.toDomain() } ?: emptyList(),
        vid = vid,
        expiresAt = expiresAt
    )
}

private fun String.parseCloudfrontCookies(): Map<String, String> =
    split(";").filter { it.contains("=") }
        .associate { pair ->
            val (k, v) = pair.trim().split("=", limit = 2)
            k.trim() to v.trim()
        }
```

---

## 7. Room Database Schema

```kotlin
// core/data/db/AppDatabase.kt
@Database(
    entities = [
        CachedTabEntity::class,
        PlaybackPositionEntity::class,
        WatchlistEntity::class,
        RecentSearchEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabCacheDao(): TabCacheDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun recentSearchDao(): RecentSearchDao
}

// --- Entities ---

@Entity(tableName = "tab_cache")
data class CachedTabEntity(
    @PrimaryKey val tabId: Int,
    val contentJson: String,       // serialized List<Content> as JSON string
    val fetchedAtMs: Long,
    val version: String
)

@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val subjectId: String,
    val contentType: String,
    val seasonIndex: Int,
    val episodeIndex: Int,
    val positionMs: Long,
    val totalDurationMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val subjectId: String,
    val title: String,
    val posterUrl: String,
    val contentType: String,
    val addedAtMs: Long
)

@Entity(tableName = "recent_searches", indices = [Index("query", unique = true)])
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val searchedAtMs: Long
)
```

---

## 8. Network Layer Specification

### 8.1 API Service Interface

```kotlin
// core/network/ApiService.kt
interface ApiService {
    @GET("wefeed-mobile-bff/tab-operating")
    suspend fun getTabContent(
        @Query("tabId") tabId: Int,
        @Query("version") version: String,
        @Query("page") page: Int
    ): ApiResponse<TabOperatingData>

    @GET("wefeed-mobile-bff/subject-api/get")
    suspend fun getSubjectDetail(
        @Query("subjectId") subjectId: String,
        @Query("se") seasonIndex: Int = 0
    ): ApiResponse<SubjectDetail>

    @GET("wefeed-mobile-bff/subject-api/play-info")
    suspend fun getPlayInfo(
        @Query("subjectId") subjectId: String,
        @Query("se") seasonIndex: Int = 0,
        @Query("ep") episodeIndex: Int = 0
    ): ApiResponse<PlayInfoData>

    @GET("wefeed-mobile-bff/app/config")
    suspend fun getAppConfig(): ApiResponse<AppConfigData>
}
```

**Base URL:** `https://api6.aoneroom.com/`

### 8.2 Signing Interceptor

```kotlin
// core/network/interceptors/SigningInterceptor.kt
class SigningInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    companion object {
        private const val KEY_B64 = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O"
        private const val KEY_VERSION = 2
        private val KEY_BYTES: ByteArray = Base64.decode(KEY_B64, Base64.DEFAULT)
        private val SUPPRESSED_LOG = setOf("x-tr-signature")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Read body without consuming it
        val bodyStr = original.body?.let { body ->
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } ?: ""

        val ts = System.currentTimeMillis() + tokenStorage.getServerTimeOffsetMs()
        val sortedPathQuery = buildSortedPathQuery(original.url)
        val accept = original.header("Accept") ?: "*/*"
        val contentType = original.header("Content-Type") ?: ""
        val bodyLen = if (bodyStr.isNotEmpty()) bodyStr.toByteArray(Charsets.UTF_8).size.toString() else ""
        val bodyMd5 = if (bodyStr.isNotEmpty()) md5Hex(bodyStr.take(102400)) else ""

        val stringToSign = listOf(
            original.method.uppercase(),
            accept,
            contentType,
            bodyLen,
            ts.toString(),
            bodyMd5,
            sortedPathQuery
        ).joinToString("\n")

        val hmacB64 = hmacMd5Base64(KEY_BYTES, stringToSign)
        val signature = "$ts|$KEY_VERSION|$hmacB64"

        // Reconstruct body if present (body can only be read once)
        val newBody = if (bodyStr.isNotEmpty()) {
            bodyStr.toRequestBody(original.body!!.contentType())
        } else original.body

        val signed = original.newBuilder()
            .method(original.method, newBody)
            .header("x-tr-signature", signature)
            .build()

        return chain.proceed(signed)
    }

    private fun buildSortedPathQuery(url: HttpUrl): String {
        val path = url.encodedPath
        if (url.querySize == 0) return path
        val sorted = url.queryParameterNames
            .sorted()
            .joinToString("&") { name ->
                val decoded = URLDecoder.decode(name, "UTF-8")
                val value = url.queryParameter(name)?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
                "$decoded=$value"
            }
        return "$path?$sorted"
    }

    private fun hmacMd5Base64(key: ByteArray, data: String): String {
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(key, "HmacMD5"))
        val digest = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun md5Hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

### 8.3 Auth Interceptor

```kotlin
// core/network/interceptors/AuthInterceptor.kt
class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getToken()
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            // Signal to the ViewModel layer via a custom header on the synthetic response
            // Real refresh logic lives in AuthViewModel observing a SharedFlow<Unit> emitted here
            tokenStorage.markTokenExpired()
        }
        return response
    }
}
```

### 8.4 ClientInfo Interceptor

```kotlin
// core/network/interceptors/ClientInfoInterceptor.kt
class ClientInfoInterceptor(
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionState: SessionState         // singleton holding profile flags
) : Interceptor {

    private val json = kotlinx.serialization.json.Json { encodeDefaults = true }

    override fun intercept(chain: Interceptor.Chain): Response {
        val clientInfo = buildClientInfoJson()
        val request = chain.request().newBuilder()
            .header("x-client-info", clientInfo)
            .header("x-child-uid", "")
            .header("x-play-mode", "2")
            .header("x-idle-data", "1")
            .header("x-family-mode", if (sessionState.isKidsProfile) "1" else "0")
            .header("x-content-mode", if (sessionState.isKidsProfile) "1" else "0")
            .header("x-client-status", "0")
            .header("User-Agent", buildUserAgent())
            .header("Accept", "*/*")
            .build()
        return chain.proceed(request)
    }

    private fun buildClientInfoJson(): String {
        val info = mapOf(
            "package_name" to "com.zinema.app",
            "version_name" to BuildConfig.VERSION_NAME,
            "version_code" to BuildConfig.VERSION_CODE,
            "os" to "android",
            "os_version" to Build.VERSION.RELEASE,
            "install_ch" to "google-play",
            "device_id" to deviceIdProvider.getHashedDeviceId(),
            "brand" to Build.BRAND,
            "model" to Build.MODEL,
            "system_language" to Locale.getDefault().language,
            "net" to getNetworkType(),
            "region" to Locale.getDefault().country,
            "timezone" to TimeZone.getDefault().id
        )
        return json.encodeToString(info)
    }

    private fun buildUserAgent(): String =
        "com.zinema.app/${BuildConfig.VERSION_CODE} (Linux; U; Android ${Build.VERSION.RELEASE}; ${Locale.getDefault()}; ${Build.MODEL}; Build/${Build.ID})"
}
```

### 8.5 CloudFront CookieJar

```kotlin
// core/network/cdn/CloudFrontCookieJar.kt
class CloudFrontCookieJar : CookieJar {

    private val cdnHosts = setOf("sacdn.hakunaymatata.com", "msacdn.hakunaymatata.com")
    private val cookies = mutableMapOf<String, List<Cookie>>()

    fun setPlayAuthCookies(host: String, policy: String, signature: String, keyPairId: String) {
        require(host in cdnHosts) { "Attempted to set CDN cookies for unauthorized host: $host" }
        cookies[host] = listOf(
            buildCookie(host, "CloudFront-Policy", policy),
            buildCookie(host, "CloudFront-Signature", signature),
            buildCookie(host, "CloudFront-Key-Pair-Id", keyPairId)
        )
    }

    fun clearCookies() = cookies.clear()

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        if (url.host in cdnHosts) cookies[url.host] ?: emptyList() else emptyList()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) { /* no-op */ }

    private fun buildCookie(host: String, name: String, value: String): Cookie =
        Cookie.Builder().domain(host).name(name).value(value).build()
}
```

### 8.6 CDN Validator

```kotlin
// core/network/cdn/CdnValidator.kt
object CdnValidator {
    private val ALLOWED_API_HOSTS = setOf("api6.aoneroom.com")
    private val ALLOWED_CDN_HOSTS = setOf(
        "pbcdn.aoneroom.com",
        "pacdn.aoneroom.com",
        "macdn.aoneroom.com",
        "sacdn.hakunaymatata.com",
        "msacdn.hakunaymatata.com",
        "cacdn.hakunaymatata.com"
    )
    // RFC-1918 + loopback prefixes — reject any of these
    private val PRIVATE_PREFIXES = listOf("10.", "192.168.", "172.16.", "172.17.", "127.", "0.")

    fun isAllowed(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val host = HttpUrl.parse(url)?.host ?: return false
            if (PRIVATE_PREFIXES.any { host.startsWith(it) }) return false
            host in ALLOWED_CDN_HOSTS || host in ALLOWED_API_HOSTS
        } catch (e: Exception) { false }
    }

    fun isStreamHost(url: String): Boolean {
        val host = HttpUrl.parse(url)?.host ?: return false
        return host == "sacdn.hakunaymatata.com" || host == "msacdn.hakunaymatata.com"
    }
}
```

### 8.7 OkHttp + Retrofit Hilt Module

```kotlin
// core/network/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideCloudFrontCookieJar(): CloudFrontCookieJar = CloudFrontCookieJar()

    @Provides @Singleton
    fun provideOkHttpClient(
        signingInterceptor: SigningInterceptor,
        authInterceptor: AuthInterceptor,
        clientInfoInterceptor: ClientInfoInterceptor,
        cookieJar: CloudFrontCookieJar,
        logScrubInterceptor: LogScrubInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(clientInfoInterceptor)   // add headers first
        .addInterceptor(authInterceptor)          // then auth
        .addInterceptor(signingInterceptor)       // signing last (needs final headers)
        .addInterceptor(logScrubInterceptor)
        .addNetworkInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        })
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        return Retrofit.Builder()
            .baseUrl("https://api6.aoneroom.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides @Singleton
    fun provideCoil(
        context: Application,
        okHttpClient: OkHttpClient
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(200L * 1024 * 1024) // 200MB
                .build()
        }
        .components {
            add(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): ImageResult {
                    val url = chain.request().data as? String ?: return chain.proceed(chain.request())
                    if (!CdnValidator.isAllowed(url)) {
                        return ErrorResult(chain.request(), Exception("CDN not in allowlist: $url"))
                    }
                    return chain.proceed(chain.request())
                }
            })
        }
        .build()
}
```

---

## 9. UI Architecture & Navigation

### 9.1 MainActivity — Platform Detector

```kotlin
// app/MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isTv = (getSystemService(UI_MODE_SERVICE) as UiModeManager)
            .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        setContent {
            ZinemaTheme {
                if (isTv) TvNavigation() else AppNavigation()
            }
        }
    }
}
```

### 9.2 Navigation Routes

```kotlin
// navigation/Screen.kt
sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object ProfileSelector : Screen("profile_selector")
    data object Home : Screen("home")
    data object Detail : Screen("detail/{subjectId}") {
        fun createRoute(subjectId: String) = "detail/$subjectId"
    }
    data object Player : Screen("player/{subjectId}/{seasonIndex}/{episodeIndex}") {
        fun createRoute(id: String, se: Int = 0, ep: Int = 0) = "player/$id/$se/$ep"
    }
    data object Search : Screen("search")
    data object ShortTv : Screen("shorttv")
}
```

### 9.3 UiState Pattern

Every ViewModel in every feature follows this exact pattern:

```kotlin
// Example: feature/home/HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTabContentUseCase: GetTabContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadTab(tabId: Int) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getTabContentUseCase(tabId)
                .catch { e -> _uiState.value = HomeUiState.Error(e.message ?: "Unknown error") }
                .collect { content -> _uiState.value = HomeUiState.Success(content) }
        }
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val rails: List<ContentRailData>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
```

---

## 10. Theme & Design System

### 10.1 Color Palette (Netflix-Style Dark)

```kotlin
// core/ui/theme/Color.kt
object ZinemaColors {
    val Background      = Color(0xFF141414)   // Netflix black
    val Surface         = Color(0xFF1F1F1F)
    val SurfaceVariant  = Color(0xFF2A2A2A)
    val Primary         = Color(0xFFE50914)   // Netflix red
    val PrimaryVariant  = Color(0xFFB20710)
    val OnBackground    = Color(0xFFFFFFFF)
    val OnSurface       = Color(0xFFE5E5E5)
    val TextSecondary   = Color(0xFF999999)
    val TextTertiary    = Color(0xFF666666)
    val FocusRing       = Color(0xFFFFFFFF)   // TV focus ring
    val RatingGold      = Color(0xFFF5C518)   // IMDb gold
    val ProgressBar     = Color(0xFFE50914)
    val Shimmer         = Color(0xFF2A2A2A)
    val ShimmerHighlight= Color(0xFF3A3A3A)
    val Overlay         = Color(0xCC000000)   // 80% black overlay
}
```

### 10.2 Typography

```kotlin
// core/ui/theme/Type.kt
val ZinemaTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp,   color = Color.White),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp,   color = Color.White),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
```

### 10.3 Key Composable Specs

**`ContentCard` (2:3 poster):**
- Width: 110dp (mobile), 160dp (TV)
- Aspect ratio: `2f / 3f`
- Corner radius: 4dp
- Hover/focus scale: 1.0 → 1.08 (animated, 150ms)
- Rating badge: bottom-left overlay, `RatingGold` text, 10sp

**`WideContentCard` (16:9 thumbnail):**
- Width: 200dp (mobile), 280dp (TV)
- Aspect ratio: `16f / 9f`
- Title overlay: bottom gradient + 12sp white text

**`HeroBanner`:**
- Height: 56% of screen height
- Gradient overlay: `Brush.verticalGradient(listOf(Color.Transparent, Background))`
- Auto-advance: `LaunchedEffect(pagerState.currentPage)` + `delay(5000)`
- Page indicator dots: bottom-center, 6dp circles, active = white, inactive = 40% white

**`ShimmerRail`:**
- Use `InfiniteTransition` animating a gradient from `Shimmer` to `ShimmerHighlight`
- Show 5 placeholder cards per rail

---

## 11. Atomic Task List

Tasks are ordered by dependency. Complete each task before starting the next in the same phase. Tasks within the same phase that have no interdependency can be parallelized.

---

### Phase 0 — Project Scaffolding (Do first, no exceptions)

```
T-001  Create root project with settings.gradle.kts listing all 9 modules
T-002  Create gradle/libs.versions.toml with all dependencies from §2.2
T-003  Create build.gradle.kts for each module using the dependency graph in §3.1
T-004  Add google-services.json placeholder (Firebase); configure Crashlytics plugin
T-005  Create AndroidManifest.xml for :app with:
         - android:label="Zinema" (app display name on launcher)
         - INTERNET, ACCESS_NETWORK_STATE permissions
         - android:supportsRtl="false"
         - android:theme="@style/Theme.SplashScreen"
         - leanbackLaunchIntent intent-filter for TV support
         - android:banner="@drawable/tv_banner" for TV launcher
T-006  Create proguard-rules.pro with rules to:
         - Keep all @Serializable classes
         - Keep Hilt-generated classes
         - Strip all logging in release (-assumenosideeffects Log.*)
         - Strip SigningInterceptor key constant from stack traces
T-007  Create .gitignore, local.properties, and google-services.json.example
```

---

### Phase 1 — Core Security & Network

```
T-008  Implement TokenStorage.kt using EncryptedSharedPreferences:
         - getToken(): String
         - saveToken(jwt: String)
         - clearToken()
         - markTokenExpired() → emits to SharedFlow<Unit> for AuthViewModel
         - getServerTimeOffsetMs(): Long
         - saveServerTimeOffsetMs(offset: Long)
         
T-009  Implement DeviceIdProvider.kt:
         - On first call: read ANDROID_ID, SHA-256 hash it, store in EncryptedSharedPreferences
         - On subsequent calls: return cached value
         
T-010  Implement SecurityModule.kt (Hilt @InstallIn(SingletonComponent::class)):
         - Bind TokenStorage as @Singleton
         - Bind DeviceIdProvider as @Singleton
         
T-011  Implement SigningInterceptor.kt exactly as specified in §8.2
         - Unit test: verify the HMAC output for a known input matches expected value
         
T-012  Implement AuthInterceptor.kt as specified in §8.3
T-013  Implement ClientInfoInterceptor.kt as specified in §8.4
T-014  Implement CloudFrontCookieJar.kt as specified in §8.5
T-015  Implement CdnValidator.kt as specified in §8.6
T-016  Implement NetworkModule.kt as specified in §8.7
         - Wire all interceptors in correct order: ClientInfo → Auth → Signing → LogScrub
T-017  Implement all DTO data classes from §5 in core/network/dto/
T-018  Implement ApiService.kt interface from §8.1
T-019  Implement LogScrubInterceptor.kt:
         - In DEBUG: allow all logs except "Authorization" header value
         - In RELEASE: use OkHttp Level.NONE
         - Never log: Authorization, x-tr-signature, Cookie, Set-Cookie header values
```

---

### Phase 2 — Domain & Data Layer

```
T-020  Implement all domain models from §6 in core/domain/model/
T-021  Implement all repository interfaces in core/domain/repository/
T-022  Implement Room entities from §7 in core/data/db/entities/
T-023  Implement DAOs for each entity with appropriate queries:
         TabCacheDao:
           - upsert(entity: CachedTabEntity)
           - getByTabId(tabId: Int): CachedTabEntity?
           - deleteOlderThan(timestampMs: Long)
         PlaybackPositionDao:
           - upsert(entity: PlaybackPositionEntity)
           - getBySubjectId(subjectId: String): PlaybackPositionEntity?
           - getAllBetweenCompletion(minPct: Float, maxPct: Float): List<PlaybackPositionEntity>
           - deleteAll()
         WatchlistDao:
           - upsert(entity: WatchlistEntity)
           - delete(subjectId: String)
           - getAll(): Flow<List<WatchlistEntity>>
           - isInWatchlist(subjectId: String): Flow<Boolean>
         RecentSearchDao:
           - upsert(entity: RecentSearchEntity)
           - getRecent(limit: Int = 10): List<RecentSearchEntity>
           - deleteAll()
           
T-024  Implement AppDatabase.kt with Room.databaseBuilder, use "zinema.db" as name
T-025  Implement ContentMapper.kt with all mappers from §6.1
T-026  Implement ContentRepositoryImpl.kt:
         - getTabContent(tabId, page): Flow<List<Content>>
           * Check cache: if exists and age < 2 hours, emit cached data then return
           * Else: call API, parse all block types (BANNER, SUBJECTS_MOVIE, CUSTOM),
             deduplicate by subjectId, map to domain, cache, emit
         - getContentDetail(subjectId): Flow<Content>
         - getStreamInfo(subjectId, seasonIndex, episodeIndex): StreamInfo
           * Always fresh — never cache
           * Validate stream_url with CdnValidator.isStreamHost()
           * If validation fails, throw StreamSecurityException
         - searchContent(query): Flow<List<Content>>
           
T-027  Implement PlaybackRepositoryImpl.kt:
         - savePosition(subjectId, se, ep, positionMs, totalMs)
         - getPosition(subjectId): PlaybackPositionEntity?
         - getContinueWatchingList(): Flow<List<Content>>
           * Returns entries where 2% < (positionMs/totalMs) < 95%
           
T-028  Implement DataModule.kt (Hilt) providing DB, all DAOs, all Repos
T-029  Implement all use cases in core/domain/usecase/ (one class per file):
         Each use case has a single operator fun invoke() that delegates to the repository
```

---

### Phase 3 — Theme & Shared UI Components

```
T-030  Implement ZinemaTheme.kt, Color.kt, Type.kt, Shape.kt from §10
         - Theme wraps both Material3 MaterialTheme and tv-material MaterialTheme
         - Detect isTV via LocalContext and apply appropriate theme surface colors
         
T-031  Implement ContentCard.kt (2:3 poster):
         - Parameters: content: Content, onClick: () -> Unit, modifier: Modifier
         - Use AsyncImage from Coil, placeholder = ShimmerEffect
         - Show RatingBadge if content.rating != null
         - TV: wrap in tv-material Card for built-in focus handling
         - Mobile: use Material3 Card with clickable modifier
         
T-032  Implement WideContentCard.kt (16:9 thumbnail) and ShortCard.kt (9:16 vertical)
T-033  Implement ContentRail.kt:
         - Parameters: title: String, items: List<Content>, onItemClick: (Content) -> Unit
         - LazyRow with 8dp item spacing, 16dp horizontal padding
         - "See All" button at rail end if items.size >= 10
         
T-034  Implement HeroBanner.kt:
         - HorizontalPager with PagerState
         - Auto-advance LaunchedEffect at 5 seconds
         - Gradient overlay
         - Play and + Watchlist buttons
         - Page indicator dots
         
T-035  Implement ShimmerRail.kt using InfiniteTransition gradient animation
T-036  Implement RatingBadge.kt, GenreChip.kt, ErrorBanner.kt, OfflineBanner.kt
```

---

### Phase 4 — Auth Feature

```
T-037  Implement AuthViewModel.kt:
         - UiState: Idle | Loading | Success(token) | Error(message)
         - loginAsGuest(): reads GUEST_JWT from BuildConfig.GUEST_JWT (set via buildConfigField)
           and saves to TokenStorage
         - loginWithCredentials(email, password): POST to OneID endpoint
         - collectTokenExpiredEvents(): navigates to login when TokenStorage signals expiry
         
T-038  Implement LoginScreen.kt:
         - Dark background matching ZinemaColors.Background
         - Centered logo (vector drawable, red Zinema wordmark)
         - Email / password fields using OutlinedTextField with dark styling
         - "Sign In" button (Primary red)
         - "Continue as Guest" text button
         - Error snackbar using Scaffold SnackbarHost
         
T-038b Create res/values/strings.xml with:
         - app_name = "Zinema"
         - tagline   = "Cinema without limits."

T-039  Implement ProfileSelectorScreen.kt:
         - Grid of profile avatars (max 5)
         - "Add Profile" + button
         - "Manage Profiles" option
         - Kids profile toggle per avatar
         - PIN entry dialog for adult-locked profiles
```

---

### Phase 5 — Home & Browse Feature

```
T-040  Implement HomeViewModel.kt and TabContentViewModel.kt from §9.3 pattern
T-041  Define CONTENT_TABS constant list (from PRD §2.4 table):
         Remove tabId=27 per OQ-06 decision.
         Each ContentTab has: tabId, displayName, keyword, icon.
         
T-042  Implement HomeScreen.kt (Mobile):
         - TopAppBar: logo left, search icon + profile icon right
         - OfflineBanner at top when no network
         - Scaffold with bottom navigation bar (5 tabs: Home, Movies, TV, Trending, ShortTV)
         - "More" tab opens full-screen category grid
         - Content area: LazyColumn of ContentRails
         - HeroBanner as first item in LazyColumn
         - shimmer loading state
         - Empty state for geo-blocked content (OQ-08)
         
T-043  Implement TvHomeScreen.kt (TV):
         - Row layout: NavigationDrawer (left) + content area (right)
         - NavigationDrawer: LazyColumn of tab icons, expands to show labels on left D-pad
         - Content area: LazyColumn of ContentRails with HeroBanner at top
         - All focusable items use tv-material Card or custom FocusRequester
         - rememberTvLazyListState() for TV-optimized list performance
```

---

### Phase 6 — Detail Feature

```
T-044  Implement DetailViewModel.kt:
         - Loads ContentDetail on init
         - Loads WatchlistState as Flow
         - toggleWatchlist()
         - getPlaybackPosition(subjectId)
         
T-045  Implement DetailScreen.kt:
         - CollapsingToolbarLayout equivalent in Compose:
           Use NestedScrollConnection + graphicsLayer alpha on backdrop
         - Backdrop image (preVideoCover.url) with gradient overlay
         - Play button (red, prominent) — direct play for movie/short/sports
         - "+ My List" and "Share" icon buttons
         - Title (H1 white), Year · Rating · Type badge row, Genre chips
         - Expandable description (2 lines collapsed, tap to expand)
         - CONDITIONAL: Season selector dropdown + Episode LazyColumn for tv/anime types
         - "More Like This" horizontal ContentRail at bottom
         
T-046  Implement EpisodeListSection.kt:
         - Season selector: ExposedDropdownMenuBox
         - Episode rows: thumbnail + title + duration + progress bar if watched
         - Tapping episode row triggers Player navigation
```

---

### Phase 7 — Player Feature

```
T-047  Implement PlayerViewModel.kt:
         - loadStreamInfo(subjectId, se, ep): calls GetStreamInfoUseCase
           * On success: call cookieJar.setPlayAuthCookies(...)
           * On StreamSecurityException: emit Error state, log to Crashlytics
         - reportPosition(positionMs, totalMs): calls SavePlaybackPositionUseCase every 5s
         - scheduleExpiryRefresh(expiresAt): if expiresAt not blank, launch coroutine
           to re-call loadStreamInfo 60 seconds before expiry
         - UiState: Loading | Ready(StreamInfo) | Error(message)
         
T-048  Implement PlayerScreen.kt (Mobile):
         - Full-screen landscape enforced via ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
         - ExoPlayer initialized based on StreamProtocol:
             DASH         → DashMediaSource
             HLS          → HlsMediaSource
             PROGRESSIVE  → ProgressiveMediaSource
         - Subtitle tracks loaded via SingleSampleMediaSource (cacdn.* URLs only)
         - Custom PlayerControls composable overlay (auto-hide after 3s)
         - Double-tap left/right seek ±10s
         - Swipe right-half = volume, swipe left-half = brightness
         - PiP mode: onUserLeaveHint() triggers enterPictureInPictureMode()
         
T-049  Implement TvPlayerScreen.kt (TV):
         - Full-screen, no orientation lock needed on TV
         - TvPlayerControls with D-pad seek (±10s with controls visible, ±30s without)
         - Next episode thumbnail row at bottom (TV series only)
         - Back button → popBackStack() to Detail screen
         
T-050  Implement PlayerControls.kt + TvPlayerControls.kt:
         - Progress slider with position/duration text
         - CC button → SubtitleTrackSelector bottom sheet
         - Quality button → QualitySelector bottom sheet
         - Both use animateContentSize() for smooth show/hide
         
T-051  Implement SubtitleTrackSelector.kt:
         - ModalBottomSheet (mobile) / Dialog (TV)
         - List of available SubtitleTrack items from StreamInfo
         - "Off" option always at top
         - Selected language persisted via DataStore
         
T-052  Implement QualitySelector.kt:
         - Available qualities parsed from streams list in PlayInfoData
         - Default pre-selected to "1080"
         - On selection: call playerViewModel.changeQuality(quality)
```

---

### Phase 8 — Search Feature

```
T-053  Implement SearchViewModel.kt:
         - query: MutableStateFlow<String>
         - results: derived StateFlow using debounce(300ms) on query
         - recentSearches: Flow<List<String>> from RecentSearchDao
         - onQuerySubmit(): saves to RecentSearchDao (max 10, evict oldest)
         - clearRecentSearches()
         
T-054  Implement SearchScreen.kt (Mobile + TV):
         - SearchBar composable at top (Material3 SearchBar)
         - When query is empty: show RecentSearches chips
         - When query has text: show results grouped by ContentType
         - TV: use Leanback-style SearchBar from tv-material
```

---

### Phase 9 — ShortTV Feature (Mobile Only)

```
T-055  Implement ShortTvViewModel.kt:
         - Loads ShortTV tab (tabId=13) content
         - Manages current visible item index
         - Triggers stream load on viewport entry
         
T-056  Implement ShortTvScreen.kt:
         - LazyColumn with snap behavior (flingBehavior = rememberSnapFlingBehavior)
         - Each item: 100% screen height, 9:16 ratio
         - AutoPlayVideoItem composable:
             ExoPlayer ProgressiveMediaSource for msacdn.* URLs
             Muted by default, tap to unmute
             Auto-play on viewport entry via VisibilityTracker + DisposableEffect
         - PreloadControl: preload next 2 items
         - Bottom overlay: title, episode count, "+ Watch Series" button (appears after 2s)
```

---

### Phase 10 — Analytics & Final Wiring

```
T-057  Implement AnalyticsTracker.kt (singleton):
         - Wraps Firebase Analytics
         - Methods: trackTabViewed(), trackContentImpressed(), trackPlayInitiated(),
           trackPlaybackStarted(ttffMs), trackBufferEvent(), trackApiRetry(),
           trackAssetBlocked(urlHash)
         - PII guard: never accept or log raw URLs, JWTs, or device IDs
         
T-058  Instrument all events from PRD §6.1 at their trigger sites:
         - app_open → MainActivity.onCreate
         - tab_viewed → HomeViewModel.loadTab
         - play_initiated → DetailViewModel before navigation
         - playback_started → PlayerViewModel on first frame
         - buffer_start / buffer_end → ExoPlayer PlaybackStateListener
         
T-059  Wire Crashlytics:
         - Add setCustomKey("platform", "tv"/"mobile") on session start
         - Add PlaybackException handler in PlayerViewModel → Crashlytics.recordException
         
T-060  Implement ConnectivityObserver.kt using NetworkCallback:
         - Exposes Flow<Boolean> isConnected
         - OfflineBanner in HomeScreen observes this flow
         
T-061  Write end-to-end smoke test for signing interceptor:
         - Given a hardcoded request, assert the x-tr-signature matches a known-good value
         - This test must pass before any release build
         
T-062  Configure network_security_config.xml:
         - In debug: allow cleartext for localhost only
         - In release: enforce HTTPS for all domains, no cleartext
         
T-063  Write the ProGuard rules for the signing key:
         - Ensure SIGNING_KEY_B64 constant is not extractable from release APK
         - Move key to native .so via JNI if security review requires it
```

---

## Final Checklist Before First Build

Before running the app for the first time, verify all of these:

- [ ] `google-services.json` is present in `:app`
- [ ] `GUEST_JWT` is set as a `buildConfigField` in the app `build.gradle.kts`
- [ ] `SIGNING_KEY_B64` is obfuscated (not in plaintext in any source file)
- [ ] All 9 Gradle modules exist and are listed in `settings.gradle.kts`
- [ ] Hilt `@HiltAndroidApp` is applied to `ZinemaApplication.kt`
- [ ] `MainActivity` extends `ComponentActivity` and is annotated `@AndroidEntryPoint`
- [ ] `network_security_config.xml` is referenced in AndroidManifest
- [ ] Signing interceptor smoke test passes (`T-061`)
- [ ] CDN validator unit test covers: allowed hosts pass, IP literals fail, private ranges fail

---

*End of Blueprint — Project Zinema v1.0*
*This document is the single source of truth for implementation. The PRD.md governs product intent; this Blueprint governs how to build it.*
