# Product Requirements Document
## Project AOneRoom — Cross-Platform Android Streaming Client

---

| Field | Value |
|---|---|
| **Document Version** | 1.0 |
| **Status** | Draft — For Engineering Review |
| **Product Owner** | TBD |
| **Last Revised** | 2026-06-29 |
| **Classification** | Internal — Confidential |

---

## Table of Contents

1. [Document Control & Executive Summary](#1-document-control--executive-summary)
2. [User Experience & Interface Requirements](#2-user-experience--interface-requirements)
3. [Functional Requirements — Epic & Feature Breakdown](#3-functional-requirements)
4. [System Security & Gateway Engineering](#4-system-security--gateway-engineering)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [Analytics, Telemetry & Observability](#6-analytics-telemetry--observability)
7. [Content Governance & Compliance](#7-content-governance--compliance)
8. [Phased Delivery Roadmap](#8-phased-delivery-roadmap)
9. [Open Questions & Decisions Log](#9-open-questions--decisions-log)

---

## 1. Document Control & Executive Summary

### 1.1 Project Identity

| Attribute | Detail |
|---|---|
| **Project Name** | Project AOneRoom |
| **Working Title** | OneRoom Streaming Client |
| **Platform Targets** | Android Mobile (API 26+), Android TV / Google TV (API 26+, Leanback) |
| **Backend Ecosystem** | `api6.aoneroom.com` — `/wefeed-mobile-bff/` gateway |
| **Signing Authority** | OneID (`ire-oneid.shalltry.com`) |
| **Primary CDN** | `sacdn.hakunaymatata.com` (streams), `pbcdn.aoneroom.com` (posters) |

---

### 1.2 Executive Summary

**Project AOneRoom** is a native Android streaming client delivering a Netflix-grade content discovery and playback experience across two distinct form factors: **Android Mobile** (touch-first, portrait browsing) and **Android TV / Google TV** (D-pad navigation, 10-foot UI, Leanback framework).

The application is a first-party consumer of the `wefeed-mobile-bff` content platform, which serves a broad catalog spanning feature films, episodic TV series, anime, short-form micro-dramas, sports highlights, music videos, educational content, Nollywood, Asian cinema, Western cinema, kids programming, gaming, and live MMA/wrestling replays. The platform is architecturally segmented into **tab-based content channels**, each backed by a dedicated server-side tab ID, and authenticated through the OneID identity infrastructure.

The client must meet the following top-level mandates:

- **Security by design.** Every outbound API request must be signed using the `x-tr-signature` HMAC-MD5 protocol. Bearer JWT tokens must be stored in Android's encrypted `EncryptedSharedPreferences`. No media asset may be loaded from an unauthorized CDN domain.
- **Performance at scale.** The user must reach the first frame of video playback within **3 seconds** on a 4G connection. Browse feeds must render and scroll at a consistent **60 fps** on mid-range devices.
- **Content-aware layout.** The UI must adapt its layout and interaction model based on the entity type returned by the API — a single-click play experience for Movies and Shorts versus a full season/episode navigation tree for TV and Anime series.
- **Resilience without degradation.** Network failures must be handled transparently with exponential backoff retries, never blocking the UI thread or surfacing raw error codes to end users.

---

### 1.3 Target Audience

| Segment | Description |
|---|---|
| **Primary** | Mobile-first viewers (18–35) who consume short-form, Nollywood, and trending content on Android phones |
| **Secondary** | Living-room viewers who use Android TV / Google TV sticks/boxes for long-form movies and episodic series |
| **Tertiary** | Families and kids, served through the dedicated Kids and Education tabs with content-mode restrictions |

---

### 1.4 Strategic Goals & Success Metrics

| Goal | KPI | Target |
|---|---|---|
| User Engagement | Daily Active Users (DAU) | +25% MoM within 90 days of launch |
| Playback Quality | Buffering ratio | < 0.5% of total watch time |
| Discovery | Content-to-play conversion rate | > 35% of browse sessions result in a play |
| Retention | 7-day return rate | > 60% of new installs |
| Stability | App crash-free session rate | > 99.5% |
| Playback Start | Time to First Frame (TTFF) | ≤ 3 seconds on LTE |

---

## 2. User Experience & Interface Requirements

### 2.1 Design Principles

1. **Content First.** Poster art, titles, and ratings must dominate the visual hierarchy. Navigation chrome is secondary.
2. **Zero Dead-Ends.** Every empty state, error state, and loading state must offer a clear path forward. Spinners are never shown without context.
3. **Platform Fidelity.** Mobile uses Material Design 3 conventions; Android TV uses the Leanback `BrowseFragment` paradigm with oversized focus states and spatial audio feedback.
4. **Instant Feedback.** Taps and D-pad selections must trigger a visual response within **100ms**, regardless of whether the underlying data has loaded.

---

### 2.2 Android Mobile Experience

#### 2.2.1 Home Screen — Portrait Browsing Feed

The primary discovery surface is a vertically scrolling feed of horizontal content rails. Each rail corresponds to one or more backend content blocks returned by the `tab-operating` endpoint.

**Required Rail Types:**

| Rail Type | Layout | Data Source |
|---|---|---|
| Hero Banner | Full-width auto-advancing carousel (16:9) | `BANNER` block type — `banner.banners[]` |
| Trending Now | Horizontal scroll — 2:3 poster cards | `SUBJECTS_MOVIE` or `APPOINTMENT_LIST` blocks |
| Genre Shelf | Horizontal scroll — 2:3 poster cards | Per-tab content blocks |
| Sports & Highlights | Horizontal scroll — 16:9 thumbnail cards | `CUSTOM` block type — `customData.items[].subject` |
| Short Clips | Horizontal scroll — 9:16 vertical cards | ShortTV tab content |
| Continue Watching | Horizontal scroll — 16:9 + progress bar | Local persistence (see §5.3) |

**Interaction Specifications:**

- Tapping a poster card navigates to the **Content Detail Screen**.
- Long-pressing a poster card surfaces a bottom sheet with: Add to Watchlist, Share, and (for logged-in users) Mark as Watched.
- The hero banner auto-advances every **5 seconds** with a swipeable manual override.
- Infinite scroll: new rails are loaded when the user reaches 80% of the current feed length.

#### 2.2.2 Content Detail Screen — Mobile

The detail screen appears on tap from any card in the browse feed. It uses a **collapsed/expanded** parallax header pattern:

```
┌──────────────────────────────────┐
│  [Backdrop / Trailer Preview]    │  ← CollapsingToolbarLayout
│  ▶ Play          + Watchlist     │
├──────────────────────────────────┤
│  Title (H1)                      │
│  Year · Genre Tags · IMDb ★ 6.1  │
│  Description (2-line collapse)   │
├──────────────────────────────────┤
│  [CONDITIONAL — TV/Anime only]   │
│  Season ▼  Episode List          │
├──────────────────────────────────┤
│  More Like This (horizontal)     │
└──────────────────────────────────┘
```

**Type-Conditional Layout Rules:**

| Content `type` | Season Selector | Episode List | Play Button Behavior |
|---|---|---|---|
| `movie` | Hidden | Hidden | Direct play |
| `short` | Hidden | Hidden | Direct play (fullscreen vertical) |
| `sports` | Hidden | Hidden | Direct play |
| `tv` | Visible | Visible (per season) | Play S1E1 or resume |
| `anime` | Visible | Visible (per season) | Play S1E1 or resume |

**Genre Tag Rendering:**
Each genre string from the comma-separated `genre` API field (e.g., `"Comedy, Horror"`) is rendered as a pill/chip using Material Design 3 `FilterChip` components. Tapping a genre chip navigates to a filtered browse view for that genre.

**IMDb Rating Display:**
The `imdbRatingValue` field (e.g., `"5.3"`) is rendered as a star icon + numeric value. If null, the rating section is hidden entirely — never show `null` or `0.0` to the user.

#### 2.2.3 Landscape Video Player — Mobile

When playback begins on mobile, the app transitions to fullscreen landscape mode. The player is built on **ExoPlayer (Media3)**.

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│              [VIDEO CONTENT AREA]                    │
│                                                      │
│  ◀ Back    [Title]                    ⚙ Settings    │
│                                                      │
│  ━━━━━━━━━━━━━━━━━━━━━━━━○━━━━  43:21 / 1:52:07     │
│  ◀◀ -10s    ▶/⏸         ▶▶ +10s                     │
│  CC  🔊  📺 Quality  ⤢ Fullscreen  ⋮ More           │
└──────────────────────────────────────────────────────┘
```

**Player Controls Requirements:**
- Double-tap left/right zones to seek ±10 seconds (YouTube-style)
- Swipe up/down on the right half to adjust volume
- Swipe up/down on the left half to adjust brightness
- Controls auto-hide after **3 seconds** of inactivity
- Picture-in-Picture (PiP) mode must be supported for Android 8.0+ on mobile

---

### 2.3 Android TV / Google TV Experience (Leanback)

#### 2.3.1 Design Mandates for the 10-Foot Interface

- **Minimum focusable target size:** 48dp × 48dp (physical)
- **Focus state:** All focusable elements must have a visible, high-contrast focus ring. Scale-up on focus by 1.08× is required.
- **Typography:** Body text minimum 18sp; rail headers minimum 22sp; hero title minimum 32sp.
- **Safe zones:** All content must respect a 5% overscan boundary.
- **Audio feedback:** D-pad navigation sounds are opt-in but must be implemented via Android TV's `SoundEffectConstants`.

#### 2.3.2 Home — Leanback BrowseFragment Layout

```
┌───────────────────────────────────────────────────────────────────┐
│ AOneRoom                                        🔍  👤  ⚙         │
├─────────────────┬─────────────────────────────────────────────────┤
│  📺 Home        │  ┌──────────────────────────────────────────┐   │
│  🔥 Trending    │  │        [HERO BANNER — focused item]      │   │
│  🎬 Movie       │  └──────────────────────────────────────────┘   │
│  📺 TV          │                                                   │
│  ✨ Anime       │  TRENDING NOW ──────────────────────────────     │
│  ⚡ ShortTV     │  [Card] [Card] [Card] [Card] [Card] [Card] ▶     │
│  🌍 Nollywood   │                                                   │
│  🀄 Asian       │  NEW MOVIES ────────────────────────────────     │
│  🤠 Western     │  [Card] [Card] [Card] [Card] [Card] [Card] ▶     │
│  👶 Kids        │                                                   │
│  🎓 Education   │  ANIME ─────────────────────────────────────     │
│  🎵 Music       │  [Card] [Card] [Card] [Card] [Card] [Card] ▶     │
│  🎮 Gaming      │                                                   │
│  ⚽ Football    │                                                   │
│  🥊 FightZone   │                                                   │
└─────────────────┴─────────────────────────────────────────────────┘
```

**D-Pad Navigation Matrix:**

| From | D-Pad Up | D-Pad Down | D-Pad Left | D-Pad Right | Select |
|---|---|---|---|---|---|
| Side nav item | Previous nav item | Next nav item | — | First card in active rail | Switch content category |
| Card in rail | Move to rail above | Move to rail below | Previous card | Next card | Open detail screen |
| Hero banner | — | First content rail | Previous banner | Next banner | Play |
| Detail screen | — | Episode list (TV only) | — | Related content | Play |

#### 2.3.3 "Who's Watching" Profile Gateway

On app launch (or after 30 minutes of inactivity on TV), the app must display a profile selection screen. This is a standard pattern for shared living-room devices.

Requirements:
- Support a minimum of 5 profile slots
- Each profile stores: display name, avatar index, Kids Mode toggle
- Profiles are stored in local `EncryptedSharedPreferences` keyed per device
- Selecting a Kids profile automatically sets `x-family-mode: 1` and `x-content-mode: 1` on all subsequent API calls, and restricts visible tabs to: Home, Kids, Education, Animation
- A PIN-protected "Adult" profile option must be available

#### 2.3.4 TV Playback Interface

TV playback uses the `PlaybackSupportFragment` from Leanback:

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                    [FULL-SCREEN VIDEO]                           │
│                                                                  │
│                                                                  │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━○━━━━━━━━  1:23:45 / 1:52:07      │
│                                                                  │
│  ◀◀        ▶/⏸        ▶▶     CC   🔊   ⚙ Quality               │
│                                                                  │
│  [Thumbnail row for next episodes — TV series only]              │
└──────────────────────────────────────────────────────────────────┘
```

- **Back button** from player → returns to Detail screen (not Home)
- **Up D-pad** during playback → reveals playback controls overlay
- **OK/Select** during playback → toggle play/pause
- **DPAD_LEFT / DPAD_RIGHT** with controls visible → seek ±10 seconds
- **DPAD_LEFT / DPAD_RIGHT** without controls visible → seek ±30 seconds (lean-back seek)

---

### 2.4 Navigation Architecture

The primary navigation model maps directly to the backend tab ID system. Each tab is a distinct navigation destination backed by one server-side content channel.

**Tab Configuration:**

| Display Name | Backend Keyword | Tab ID | Platform Visibility | Icon |
|---|---|---|---|---|
| Home | `popular` | 0 | Mobile + TV | House |
| Trending | `trending` | 1 | Mobile + TV | Fire |
| Movies | `movie` | 2 | Mobile + TV | Film clapper |
| TV Shows | `tv` | 5 | Mobile + TV | TV screen |
| Anime | `anime` | 8 | Mobile + TV | Star |
| ShortTV | `short` | 13 | Mobile only (bottom nav) | Lightning bolt |
| Nollywood | `nollywood` | 28 | Mobile + TV | Globe |
| Asian | `asian` | 18 | Mobile + TV | Cherry blossom |
| Western | `western` | 19 | Mobile + TV | Cowboy hat |
| Kids | `kids` | 23 | Mobile + TV | Star balloon |
| Education | `education` | 3 | Mobile + TV | Graduation cap |
| Music | `music` | 4 | Mobile + TV | Musical note |
| Gaming | `game` | 11 | Mobile + TV | Gamepad |
| Football | `football` | 33 | Mobile + TV | Football |
| FightZone | `fightzone` | 37 | Mobile + TV | Boxing glove |

**Mobile Navigation Model:**
- Bottom navigation bar carries the top 5 tabs: Home, Movies, TV, Trending, ShortTV
- All remaining tabs are accessible via a "More" tab that expands into a full-screen category grid
- ShortTV is a standalone swipeable vertical feed in the bottom nav (TikTok-style scroll)

**Android TV Navigation Model:**
- Left-side `VerticalGridFragment`-based navigation drawer containing all 15 tabs
- Initially collapsed to icon-only; expands on left D-pad from the content grid
- Category order is configurable server-side; client re-orders based on API tab metadata

---

## 3. Functional Requirements

### Epic F-01: User Authentication

#### F-01.1 — OneID Login Flow

**User Story:** As a new user, I want to log in with my OneID credentials so that I can access personalized and full-quality content.

| ID | Requirement | Priority |
|---|---|---|
| F-01.1.1 | The app must support OneID authentication via username/password, returning a Bearer JWT | P0 |
| F-01.1.2 | The app must support guest account mode using a pre-issued guest JWT stored in app config | P0 |
| F-01.1.3 | Login screen must present email/phone field, password field, and "Continue as Guest" option | P0 |
| F-01.1.4 | Login errors (invalid credentials, network failure) must display human-readable messages | P0 |
| F-01.1.5 | The app must support biometric (fingerprint / face) authentication to resume a session | P1 |
| F-01.1.6 | Social login (Google Sign-In) must be evaluated for a v2 milestone | P2 |

#### F-01.2 — Session Persistence & JWT Security

| ID | Requirement | Priority |
|---|---|---|
| F-01.2.1 | The Bearer JWT must be stored **exclusively** in Android `EncryptedSharedPreferences` (AES256-GCM backed by the Android Keystore). Plain SharedPreferences and SQLite storage for tokens are prohibited. | P0 |
| F-01.2.2 | The app must parse the JWT's `exp` claim on every cold start. If `exp` is within 24 hours of expiry, a background token refresh must be triggered before the user reaches the Home screen. | P0 |
| F-01.2.3 | If a `401 Unauthorized` response is received during any API call, the app must attempt a silent token refresh once. If the refresh fails, the user must be redirected to the login screen with a non-blocking snackbar message. | P0 |
| F-01.2.4 | The JWT must never appear in application logs, Crashlytics reports, or analytics event payloads. Log scrubbing middleware must be implemented in the network layer. | P0 |
| F-01.2.5 | Sign-out must clear the JWT, all profile data, all playback position records, and all locally cached catalog files. | P0 |

---

### Epic F-02: The Media Discovery Engine

#### F-02.1 — Content Catalog Browsing

| ID | Requirement | Priority |
|---|---|---|
| F-02.1.1 | Each tab must invoke `GET /wefeed-mobile-bff/tab-operating?tabId={id}&version={token}&page={n}` to load its content feed | P0 |
| F-02.1.2 | The client must persist the `data.version` token returned in each tab response and send it on all subsequent page requests for that tab within the same session | P0 |
| F-02.1.3 | Content blocks must be rendered according to their `type` field: `BANNER` → hero carousel, `SUBJECTS_MOVIE` → standard horizontal rail, `CUSTOM` → specialty card format, `SPORT_LIVE` → live game scorecard widget | P0 |
| F-02.1.4 | Pagination is required: new pages must be fetched when the user reaches the end of any rail or at 80% scroll depth in a vertical feed | P0 |
| F-02.1.5 | The app must support a maximum of 5 pages per tab per session before displaying a "Load More" button to prevent unbounded data consumption | P1 |
| F-02.1.6 | Content cards must display: poster image (`cover.url`), title, and IMDb rating pill (`imdbRatingValue`). Genre tags are optional at card level but required on the detail screen. | P0 |

#### F-02.2 — Content Detail Screen

| ID | Requirement | Priority |
|---|---|---|
| F-02.2.1 | Tapping any content card must invoke `GET /wefeed-mobile-bff/subject-api/get?subjectId={id}&se=0` to fetch the full detail payload | P0 |
| F-02.2.2 | The detail screen must display: title, full description, release date, year, comma-parsed genre chips, IMDb rating, content type badge, and backdrop image | P0 |
| F-02.2.3 | For `subjectType` `tv` or `anime`: display a season selector dropdown and a scrollable episode list beneath the synopsis | P0 |
| F-02.2.4 | For `subjectType` `movie`, `short`, or `sports`: display a single prominent Play button; no episode UI elements should be rendered | P0 |
| F-02.2.5 | The detail screen must display a trailer preview if a trailer URL on `macdn.aoneroom.com` is present in the response | P1 |
| F-02.2.6 | "Add to Watchlist," "Share," and "More Like This" must be available on all detail screens | P1 |
| F-02.2.7 | For episodes, tapping an episode row must invoke the play flow for that specific episode using its `se` (season index) and `ep` (episode index) parameters | P0 |

#### F-02.3 — Search

| ID | Requirement | Priority |
|---|---|---|
| F-02.3.1 | Global search must be accessible from a persistent search icon in the top app bar (mobile) and the top-right action button (TV) | P0 |
| F-02.3.2 | Search results must display the same card components as the browse feed, organized into content-type sections | P0 |
| F-02.3.3 | Search input on Android TV must use the Leanback `SearchFragment` with the platform's speech recognition support | P0 |
| F-02.3.4 | Recent searches must be stored locally (maximum 10 entries) and displayed when the search field is focused with no query | P1 |

---

### Epic F-03: Advanced Video Player

The video player is the core value-delivery surface of the application. It must be implemented using **ExoPlayer (AndroidX Media3)** and support both long-form (MPEG-DASH / HLS) and short-form (progressive MP4) playback.

#### F-03.1 — Stream Acquisition

Before playback can begin, the app must acquire a valid stream manifest and its associated access credentials.

**Playback Initiation Flow:**

```
User presses Play
       │
       ▼
Call play-info API:
  GET /wefeed-mobile-bff/subject-api/play-info
       ?subjectId={id}&se={season_index}&ep={episode_index}
       │
       ▼
Parse response → data.streams[]
  Select entry where resolutions == "1080"
  Fallback: last entry in array (highest available)
       │
       ├── stream_url → DASH manifest on sacdn.hakunaymatata.com  (long-form)
       │                  OR MP4 on msacdn.hakunaymatata.com       (ShortTV)
       └── signCookie → CloudFront auth cookie string
               │
               ▼
       Inject play_auth as HTTP cookie header on all CDN requests
               │
               ▼
       Initialize ExoPlayer MediaSource → start buffering → first frame
```

| ID | Requirement | Priority |
|---|---|---|
| F-03.1.1 | The app must call `subject-api/play-info` every time a play action is triggered (never cache stream URLs across sessions — they carry time-limited signed cookies) | P0 |
| F-03.1.2 | The client must validate that the `stream_url` hostname is `sacdn.hakunaymatata.com` or `msacdn.hakunaymatata.com` before initializing the player. Any other host must trigger a `StreamSecurityException` and log the violation. | P0 |
| F-03.1.3 | For long-form content (`movie`, `tv`, `anime`): use ExoPlayer's `DashMediaSource` for `.mpd` URLs and `HlsMediaSource` for `.m3u8` URLs | P0 |
| F-03.1.4 | For short-form content (`short`, `sports`): use `ProgressiveMediaSource` for direct MP4 URLs from `msacdn.hakunaymatata.com` | P0 |
| F-03.1.5 | Stream quality must default to `1080p`. User must be able to manually select from available quality options in the player settings menu. | P0 |
| F-03.1.6 | If `expires_at` is present and non-empty in the stream response, the player must register a hook to gracefully end playback and re-fetch stream credentials 60 seconds before expiry | P1 |

#### F-03.2 — CloudFront Cookie Authentication

The `play_auth` field contains a multi-part CloudFront signed cookie string in the format:

```
CloudFront-Policy=<base64>;CloudFront-Signature=<sig>;CloudFront-Key-Pair-Id=<key_id>;
```

| ID | Requirement | Priority |
|---|---|---|
| F-03.2.1 | The app must parse the `play_auth` string into its three component key-value pairs and inject them as `Cookie` HTTP headers on **every** request made to `sacdn.hakunaymatata.com` or `msacdn.hakunaymatata.com` | P0 |
| F-03.2.2 | Cookie injection must be implemented at the OkHttp `CookieJar` level, not at the ExoPlayer `DataSource` level, to ensure all segment requests (manifest, init segment, media segments) are covered | P0 |
| F-03.2.3 | The `play_auth` string must never be logged, stored to disk, or included in crash reports | P0 |
| F-03.2.4 | A `403 Forbidden` response from the CDN during playback must trigger a background stream credential refresh and seamless player resume without user interruption | P1 |

#### F-03.3 — Subtitles & Closed Captions

| ID | Requirement | Priority |
|---|---|---|
| F-03.3.1 | Subtitle files are served from `cacdn.hakunaymatata.com`. The player must support SRT, VTT, and TTML formats. | P0 |
| F-03.3.2 | Available subtitle tracks must be fetched from the content detail response and displayed in the player's CC menu | P0 |
| F-03.3.3 | User subtitle language preference must persist across sessions in local preferences | P1 |
| F-03.3.4 | Subtitle rendering must support font size adjustment (Small / Medium / Large) | P1 |
| F-03.3.5 | Subtitle styling must default to white text on a semi-transparent dark background, following WCAG 2.1 Level AA contrast requirements | P0 |

#### F-03.4 — Short-Form Vertical Player (ShortTV Tab — Mobile Only)

The ShortTV tab delivers micro-drama content (subjectType 7) from `msacdn.hakunaymatata.com` and uses a distinct vertical swipe player experience.

| ID | Requirement | Priority |
|---|---|---|
| F-03.4.1 | The ShortTV feed uses a full-screen vertical `RecyclerView` (snap-to-item) where each item occupies 100% of viewport height in 9:16 aspect ratio | P0 |
| F-03.4.2 | Video must auto-play when a card enters the viewport and auto-pause when it leaves. Audio is muted by default; user tap unmutes. | P0 |
| F-03.4.3 | Tapping the content card navigates to the full episode list for the series | P0 |
| F-03.4.4 | A persistent "Play Episode 1 from ₊ episodes" bottom sheet appears after 2 seconds of viewing a teaser | P1 |
| F-03.4.5 | Pre-load the next 2 items in the feed using ExoPlayer's `PreloadControl` to eliminate buffering between swipes | P1 |

---

### Epic F-04: Watchlist, Continue Watching & User State

| ID | Requirement | Priority |
|---|---|---|
| F-04.1 | Users must be able to add any content to a Watchlist. Watchlist state is stored in local encrypted storage and optionally synced to the user's OneID account. | P1 |
| F-04.2 | Playback position must be recorded locally every 5 seconds of watch time. A "Continue Watching" rail must appear on the Home screen for content with a saved position between 2% and 95% completion. | P0 |
| F-04.3 | Content marked as > 95% complete must be hidden from Continue Watching and displayed in a "Watched" history section. | P1 |
| F-04.4 | Playback history and Watchlist must be clearable from the Profile / Settings screen. | P0 |

---

## 4. System Security & Gateway Engineering

### 4.1 The API Request Signing Protocol

Every HTTP request originating from the application to `api6.aoneroom.com` **must** include the `x-tr-signature` header. This requirement has no exceptions, including authentication calls, configuration polling, and health checks.

#### 4.1.1 Signature Construction Specification

The Android network layer must implement the following signing algorithm natively in Kotlin/Java:

**Step 1 — Collect signing inputs:**

| Field | GET Request | POST Request |
|---|---|---|
| `METHOD` | `"GET"` | `"POST"` |
| `accept` | `"*/*"` (OkHttp default) | `"*/*"` |
| `content_type` | `""` (empty) | `"application/json"` |
| `body_length` | `""` (empty) | Decimal string of UTF-8 byte count |
| `timestamp_ms` | `System.currentTimeMillis()` | `System.currentTimeMillis()` |
| `body_md5_hex` | `""` (empty) | MD5 hex of first 102,400 chars of body |
| `sorted_path_query` | Alphabetically sorted query params | Same |

**Step 2 — Assemble the string-to-sign:**

```kotlin
val stringToSign = listOf(
    method.uppercase(),
    accept,
    contentType,
    bodyLength,
    timestampMs.toString(),
    bodyMd5Hex,
    sortedPathAndQuery   // path + "?" + params sorted by key, URL-decoded before sort
).joinToString("\n")
```

**Step 3 — Compute HMAC-MD5:**

```kotlin
val keyBytes = Base64.decode(SIGNING_KEY_B64, Base64.DEFAULT)
val mac = Mac.getInstance("HmacMD5")
mac.init(SecretKeySpec(keyBytes, "HmacMD5"))
val digest = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
val hmacB64 = Base64.encodeToString(digest, Base64.NO_WRAP)
```

**Step 4 — Format the header:**

```
x-tr-signature: {timestampMs}|2|{hmacB64}

Example:
x-tr-signature: 1782745262493|2|p0vS2CWSQ/RL4637yoTQXw==
```

#### 4.1.2 Implementation Requirements

| ID | Requirement | Priority |
|---|---|---|
| S-01 | Signing must be implemented as an OkHttp `Interceptor` applied at the network layer, ensuring **all** requests — including those from Retrofit, Coil image loader (for API calls), and manual HttpUrl — are signed | P0 |
| S-02 | The signing key (`SIGNING_KEY_B64`) must be stored in the Android Keystore or obfuscated via ProGuard/R8 and native `.so` JNI layer. It must not appear in plaintext in any `.kt`, `.java`, `.xml`, or `BuildConfig` file. | P0 |
| S-03 | The query parameter sorting must URL-decode keys and values before sorting (e.g., `%2C` becomes `,`), then reconstruct the query string without re-encoding | P0 |
| S-04 | Clock skew tolerance: if the device clock is more than 5 minutes off from server time (detected via a `407 Signature Invalid` response), the app must fetch server time via the `/wefeed-mobile-bff/app/config` endpoint and apply an offset to all subsequent signatures | P1 |
| S-05 | The OkHttp interceptor must log the signature **header name only** (never the value) in debug builds. All signature logging must be stripped in release builds via ProGuard rules. | P0 |

---

### 4.2 Device Fingerprint Header — `x-client-info`

Every request must include the `x-client-info` header populated with a JSON device profile:

```json
{
  "package_name": "com.aoneroom.app",
  "version_name": "{BuildConfig.VERSION_NAME}",
  "version_code": {BuildConfig.VERSION_CODE},
  "os": "android",
  "os_version": "{Build.VERSION.RELEASE}",
  "install_ch": "google-play",
  "device_id": "{Settings.Secure.ANDROID_ID, hashed with SHA-256}",
  "brand": "{Build.BRAND}",
  "model": "{Build.MODEL}",
  "system_language": "{Locale.getDefault().language}",
  "net": "{WIFI | MOBILE | ETHERNET}",
  "region": "{Locale.getDefault().country}",
  "timezone": "{TimeZone.getDefault().id}"
}
```

**Note on Device ID:** `ANDROID_ID` must be SHA-256 hashed before inclusion. Raw hardware identifiers must not be transmitted, in compliance with Google Play's User Data Policy.

Additional session-state headers required on every request:

```
x-child-uid:      ""                   (empty unless child profile active)
x-play-mode:      "2"
x-idle-data:      "1"
x-family-mode:    "0"                  (set to "1" for Kids profile)
x-content-mode:   "0"                  (set to "1" for Kids profile)
x-client-status:  "0"
```

---

### 4.3 CDN Domain Enforcement — Asset Security Boundary

The application must enforce a strict allowlist for all media asset loading. Any URL that does not originate from an authorized CDN domain must be **silently discarded** and replaced with a placeholder.

**Authorized CDN Domains:**

| CDN Subdomain Pattern | Asset Type | Used By |
|---|---|---|
| `pbcdn.aoneroom.com` | Poster / cover art | Image loading (Coil/Glide) |
| `pacdn.aoneroom.com` | Avatar / profile images | Image loading |
| `macdn.aoneroom.com` | Trailers (MP4) | ExoPlayer `ProgressiveMediaSource` |
| `sacdn.hakunaymatata.com` | Long-form DASH/HLS streams | ExoPlayer `DashMediaSource` / `HlsMediaSource` |
| `msacdn.hakunaymatata.com` | Short-form MP4 | ExoPlayer `ProgressiveMediaSource` |
| `cacdn.hakunaymatata.com` | Subtitle files | ExoPlayer `SingleSampleMediaSource` |

| ID | Requirement | Priority |
|---|---|---|
| S-06 | The image loading layer (Coil or Glide) must register a custom `RequestInterceptor` / `ModelLoader` that validates the hostname of every URL before making a network request | P0 |
| S-07 | ExoPlayer's `DataSource.Factory` must be wrapped with a hostname-checking `DataSource` that throws `SecurityException` for any CDN request not matching the authorized list | P0 |
| S-08 | IP address literals in media URLs must be rejected unconditionally, including private RFC-1918 ranges | P0 |
| S-09 | Any rejected URL must generate a local analytics event (`asset_blocked`, `url_hash`, `context`) for security monitoring. The full URL must never be logged. | P1 |

---

## 5. Non-Functional Requirements

### 5.1 Performance Requirements

| Metric | Target | Measurement Condition |
|---|---|---|
| **Time to First Frame (TTFF)** | ≤ 3,000ms | LTE connection, 1080p DASH, cold player start |
| **TTFF — ShortTV MP4** | ≤ 1,500ms | LTE connection, pre-loaded next item |
| **App Cold Start** | ≤ 2,000ms | Pixel 5 (reference device), Android 13 |
| **App Warm Start** | ≤ 500ms | Same device |
| **Home Screen Render** | ≤ 800ms | First 2 rails visible after warm start |
| **Browse Feed FPS** | ≥ 60fps | During active scroll, measured via Perfetto |
| **TV Browse FPS** | ≥ 60fps | D-pad navigation between rails |
| **Image Load Time** | ≤ 300ms | Poster image from `pbcdn.aoneroom.com`, 300×450px |
| **Detail Screen Open** | ≤ 400ms | From card tap to hero image visible |
| **Memory Ceiling — Mobile** | ≤ 256MB | During active playback of 1080p DASH stream |
| **Memory Ceiling — TV** | ≤ 384MB | During active playback, Leanback UI rendered |

**Image Loading Strategy:**
- Use **Coil** (Kotlin-native, coroutine-aware) as the image loading library
- All poster images must be cached to disk using Coil's `DiskCache` with a 200MB limit and 7-day TTL
- Posters must be loaded at the card's rendered size (not full resolution) using Coil's `size()` modifier
- Low-resolution placeholder blurs must be rendered from the `averageHueLight` / `averageHueDark` color values in the `cover` object

---

### 5.2 Network Resilience Requirements

| ID | Requirement | Priority |
|---|---|---|
| NR-01 | The HTTP client must implement exponential backoff with jitter for all requests returning `429`, `500`, `502`, `503`, or `504` | P0 |
| NR-02 | Backoff schedule: Attempt 0 → ~0.5s, Attempt 1 → ~1.0s, Attempt 2 → ~2.0s, Attempt 3 → ~4.0s; maximum 4 total attempts | P0 |
| NR-03 | Retries must **never block the UI thread**. All network operations must execute on a background coroutine dispatcher (`Dispatchers.IO`). | P0 |
| NR-04 | During a retry cycle, the UI must display a non-blocking loading indicator (skeleton shimmer) — never a modal error dialog that prevents the user from navigating away | P0 |
| NR-05 | `401 Unauthorized` and `407 Signature Invalid` responses must **not** be retried. They must immediately surface to the business logic layer for handling. | P0 |
| NR-06 | The OkHttp client must be constructed with `trust_env = false` equivalent — do not respect system proxy settings. The `useDefaultSSLSocketFactory` behavior must be explicitly controlled. | P0 |
| NR-07 | Network request timeouts: connect = 10s, read = 20s, write = 20s | P0 |
| NR-08 | The app must detect no-network state via `ConnectivityManager` and display an offline banner rather than letting requests fail silently | P0 |

---

### 5.3 Data Persistence & Local Caching

| ID | Requirement | Priority |
|---|---|---|
| DP-01 | Tab content from the last successful fetch must be persisted locally using **Room** database or JSON files in the app's internal storage, allowing the Home screen to render stale content instantly while fresh data loads | P0 |
| DP-02 | Stale cached content is considered valid for **2 hours**. After 2 hours, the app fetches fresh data on next launch and shows the cached data until the refresh completes | P0 |
| DP-03 | The cache must be per-tab: stale Movie tab data must not prevent the Anime tab from displaying fresh content | P0 |
| DP-04 | Playback positions (Continue Watching state) must be stored in Room with schema: `(subjectId TEXT, contentType TEXT, positionMs INTEGER, totalDurationMs INTEGER, timestamp INTEGER)` | P0 |
| DP-05 | All locally stored content metadata (titles, descriptions, URLs) must be stored in Android's internal storage (not external/SD card) | P0 |
| DP-06 | Stream URLs and `play_auth` cookies must **never** be persisted to disk. They must exist in memory only for the duration of a playback session. | P0 |
| DP-07 | Cache eviction strategy: LRU eviction when combined cached catalog size exceeds 50MB | P1 |

---

### 5.4 Accessibility Requirements

| ID | Requirement | Priority |
|---|---|---|
| A-01 | All interactive elements on mobile must have a minimum touch target of 48×48dp | P0 |
| A-02 | All images must provide `contentDescription` values populated from the content `title` field | P0 |
| A-03 | The app must support Android TalkBack screen reader on mobile and D-pad focus navigation (already required) on TV | P0 |
| A-04 | Text contrast ratio must meet WCAG 2.1 Level AA (4.5:1 for body text, 3:1 for large text) | P0 |
| A-05 | The player must support system-level caption preferences on both mobile and TV | P1 |
| A-06 | Font scaling: the app must not override `fontScale` system settings; all `sp`-unit text must scale correctly up to 200% | P1 |

---

## 6. Analytics, Telemetry & Observability

### 6.1 Required Analytics Events

The following events must be instrumented at launch. All events must be routed through a server-side analytics collection endpoint (Firebase Analytics or equivalent).

| Event Name | Trigger | Required Properties |
|---|---|---|
| `app_open` | App foreground | `profile_type`, `is_cold_start`, `platform` |
| `tab_viewed` | User selects a content tab | `tab_name`, `tab_id` |
| `content_impressed` | A content card enters the viewport | `subject_id`, `title`, `content_type`, `tab_name`, `position_in_feed` |
| `content_detail_opened` | User taps a content card | `subject_id`, `title`, `content_type` |
| `play_initiated` | User presses the Play button | `subject_id`, `quality`, `content_type`, `season`, `episode` |
| `playback_started` | First frame rendered | `subject_id`, `ttff_ms`, `stream_protocol` (DASH/HLS/MP4) |
| `playback_error` | Player reports an error | `subject_id`, `error_code`, `error_message_generic`, `stream_protocol` |
| `buffer_start` | Player enters buffering state | `subject_id`, `position_ms` |
| `buffer_end` | Player exits buffering state | `subject_id`, `buffer_duration_ms` |
| `playback_completed` | Video reaches end | `subject_id`, `total_duration_ms` |
| `quality_changed` | User selects a different quality | `subject_id`, `old_quality`, `new_quality` |
| `subtitle_toggled` | User enables/disables CC | `subject_id`, `language`, `enabled` |
| `search_query` | User submits a search | `query_length` (not the query text), `result_count` |
| `watchlist_add` | User adds content to Watchlist | `subject_id`, `content_type` |
| `api_retry` | Network retry triggered | `endpoint_path` (no query params), `retry_attempt`, `http_status` |
| `asset_blocked` | CDN allowlist blocked a URL | `url_hash` (SHA-256 of URL, never raw URL), `context` |

### 6.2 Crash & Error Monitoring

- **Firebase Crashlytics** must be integrated for all builds
- Crashlytics must be initialized with `setCustomKey("content_type", ...)` and `setCustomKey("platform", "mobile"/"tv")` on every session
- PII scrubbing must be applied: JWT values, device IDs, and full media URLs must never appear in crash reports
- All `PlaybackException` instances must include `errorCode` and `errorCodeName` in the Crashlytics breadcrumb trail

---

## 7. Content Governance & Compliance

### 7.1 Parental Controls & Kids Mode

| ID | Requirement | Priority |
|---|---|---|
| CG-01 | Kids Profile must set `x-family-mode: 1` and `x-content-mode: 1` on all API requests, relying on the server to filter inappropriate content | P0 |
| CG-02 | The Kids profile must restrict tab navigation to: Home, Kids (tabId=23), Education (tabId=3), and Anime (tabId=8) | P0 |
| CG-03 | Switching out of a Kids Profile must require PIN entry | P0 |
| CG-04 | The Kids profile UI must use a distinct visual theme (rounded corners, bright palette, large typography) | P1 |

### 7.2 Legal & Platform Compliance

| ID | Requirement | Priority |
|---|---|---|
| CG-05 | The app must comply with Google Play's **Families Policy** for any content accessible to users under 13, including the Kids tab | P0 |
| CG-06 | The app must not facilitate downloading of stream content to device storage. The "download" capability is explicitly out of scope for v1.0. | P0 |
| CG-07 | The app's `network_security_config.xml` must pin TLS certificates for `api6.aoneroom.com` and CDN domains in production builds | P1 |
| CG-08 | All analytics data collection must comply with GDPR and CCPA requirements. A consent dialog must be shown on first launch in applicable regions. | P0 |

---

## 8. Phased Delivery Roadmap

### Phase 1 — Foundation (Milestone: Public Beta)

**Target:** Core authenticated browse + playback experience on both platforms.

| Feature | Target Platform |
|---|---|
| OneID authentication (JWT, guest mode) | Mobile + TV |
| Home, Trending, Movies, TV, Anime tabs | Mobile + TV |
| Content Detail screen (all types) | Mobile + TV |
| DASH/HLS long-form playback | Mobile + TV |
| Leanback BrowseFragment navigation | TV |
| Portrait browsing feed | Mobile |
| CloudFront cookie injection | Mobile + TV |
| x-tr-signature signing interceptor | Mobile + TV |
| CDN domain enforcement | Mobile + TV |
| Local catalog caching (2-hour TTL) | Mobile + TV |
| Continue Watching | Mobile + TV |
| Subtitles (VTT/SRT) | Mobile + TV |
| ExoPlayer quality selector | Mobile + TV |
| Search (basic) | Mobile + TV |

---

### Phase 2 — Content Breadth & Engagement (Milestone: General Availability)

| Feature | Target Platform |
|---|---|
| All 15 content tabs | Mobile + TV |
| ShortTV vertical feed (TikTok-style) | Mobile |
| Watchlist | Mobile + TV |
| "Who's Watching" profile gateway | TV |
| Biometric session resume | Mobile |
| Picture-in-Picture | Mobile |
| Trailer preview on detail screen | Mobile + TV |
| Genre-filtered browse | Mobile + TV |
| Kids Profile + PIN protection | Mobile + TV |
| Watchlist cloud sync (OneID) | Mobile + TV |

---

### Phase 3 — Quality & Platform Excellence

| Feature | Target Platform |
|---|---|
| Offline content download (metadata only — no streams) | Mobile |
| TLS certificate pinning | Mobile + TV |
| Clock skew correction for signature | Mobile + TV |
| Stream expiry pre-fetch (60s before `expires_at`) | Mobile + TV |
| Advanced subtitle styling | Mobile + TV |
| Portrait/landscape-adaptive player | Mobile |
| Chromecast / Google Cast sender | Mobile |
| Android Auto metadata display | Mobile |

---

## 9. Open Questions & Decisions Log

| ID | Question | Owner | Status |
|---|---|---|---|
| OQ-01 | What is the server-side clock skew tolerance window for `x-tr-signature` timestamp validation? Engineering must confirm to implement client-side offset logic. | Backend Eng | Open |
| OQ-02 | Is the `x-client-info.device_id` field validated server-side against the issuing device? If so, rotating a hashed `ANDROID_ID` per app install may cause authentication failures. | Security + Backend | Open |
| OQ-03 | What is the strategy for rotating the HMAC signing key (`_KEY_VERSION`)? Does the app need to support in-flight key rotation via a config endpoint? | Backend Eng | Open |
| OQ-04 | Does the `subject-api/play-info` endpoint require additional `x-play-mode` or `x-content-mode` values for premium or locked content? | Backend Eng | Open |
| OQ-05 | Is there a rate limit on `tab-operating` requests? If yes, what is the per-device-per-minute ceiling? Required to calibrate retry/backoff parameters. | Backend Eng | Open |
| OQ-06 | Confirm the behavior of `tabId=27` (Live). The current backend maps this to an H5 external link (`sportsnow.top`). Should the mobile app deep-link out, embed a WebView, or suppress this tab entirely? | Product | Open |
| OQ-07 | What content ID field should be used as the stable identifier for Continue Watching persistence — `subjectId` (platform) or a PocketBase record ID? | Architecture | Open |
| OQ-08 | Are there content region restrictions enforced server-side (geo-blocking)? If so, should the app display region-unavailable messaging or silently hide restricted content? | Legal + Product | Open |

---

*End of Document — Project AOneRoom PRD v1.0*
