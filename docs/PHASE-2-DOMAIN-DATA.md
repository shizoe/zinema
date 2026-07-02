# Phase 2 — Domain & Data Layer (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:core:domain` and
> `:core:data` compile; Room KSP validated every DAO query and exported the schema
> (`core/data/schemas/.../1.json`); `:app:assembleDebug` links; and the **full Hilt
> graph was verified end-to-end** with a temporary `@EntryPoint` (use case →
> repository → DAO/`ApiService` → DB/network), then removed.
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §6, §6.1, §7, and §11 → "Phase 2 —
> Domain & Data Layer" (tasks **T-020 … T-029**).

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-020** | Domain models (§6) | ✅ | `core/domain/model/` — Content, Episode, StreamInfo, ContentTab, UserProfile (+ PlaybackPosition) |
| **T-021** | Repository interfaces | ✅ | `core/domain/repository/` — Content/Playback/User |
| **T-022** | Room entities (§7) | ✅ | `core/data/db/entities/` — 4 entities |
| **T-023** | DAOs + queries | ✅ | `core/data/db/daos/` — 4 DAOs |
| **T-024** | `AppDatabase` ("zinema.db") | ✅ | [AppDatabase.kt](../core/data/src/main/kotlin/com/zinema/app/core/data/db/AppDatabase.kt), [Converters.kt](../core/data/src/main/kotlin/com/zinema/app/core/data/db/Converters.kt) |
| **T-025** | `ContentMapper` (§6.1) | ✅ | [ContentMapper.kt](../core/data/src/main/kotlin/com/zinema/app/core/data/mappers/ContentMapper.kt) (+ EntityMapper.kt) |
| **T-026** | `ContentRepositoryImpl` | ✅ | [ContentRepositoryImpl.kt](../core/data/src/main/kotlin/com/zinema/app/core/data/repositories/ContentRepositoryImpl.kt) |
| **T-027** | `PlaybackRepositoryImpl` | ✅ | [PlaybackRepositoryImpl.kt](../core/data/src/main/kotlin/com/zinema/app/core/data/repositories/PlaybackRepositoryImpl.kt) |
| **T-028** | `DataModule` (Hilt) | ✅ | [DataModule.kt](../core/data/src/main/kotlin/com/zinema/app/core/data/DataModule.kt) (+ `UserRepositoryImpl`) |
| **T-029** | Use cases (one per file) | ✅ | `core/domain/usecase/` — 7 use cases |

---

## 2. How the data layer behaves

- **Tab cache (T-026).** `getTabContent` checks `tab_cache`; if the row is < **2h**
  old it emits the cached list and stops, otherwise it fetches, flattens every
  block type (`BANNER` → `banner.banners`, `CUSTOM` → `customData.items`, else
  `subjects`), dedups by `subjectId`, maps to domain, **re-caches**, and emits.
  Domain `Content` is annotation-free, so it's persisted via a `@Serializable`
  twin, `ContentCacheModel` (JSON in `contentJson`).
- **Stream security (T-026).** `getStreamInfo` is never cached; after mapping it
  asserts `CdnValidator.isStreamHost(url)` and throws `StreamSecurityException`
  otherwise.
- **Continue Watching (T-027).** `getAllBetweenCompletion(0.02, 0.95)` returns rows
  whose `positionMs/totalDurationMs` is strictly between 2% and 95% (REAL cast
  guards integer division and zero-length items).
- **Watchlist.** `toggleWatchlist` adds or removes based on a one-shot `existsOnce`
  check; `isInWatchlist` is a reactive `Flow<Boolean>`.
- **Recent searches.** `@Insert(REPLACE)` on the unique `query` index dedups
  re-searches.

DI: `DataModule` is an abstract `@Module` with `@Binds` for the three repositories
and a `companion object` of `@Provides` for the DB + DAOs. Repos are `@Singleton`.

---

## 3. Deviations & decisions (continuing from D-19)

- **D-20 — Added `PlaybackPosition` domain model.** §6 has no resume model, but the
  `PlaybackRepository.getPosition` contract must not leak the Room entity into
  `core:domain`.
- **D-21 — Added `StreamSecurityException`** (`core/domain/exception/`), thrown by
  `getStreamInfo` and (later) caught by the player.
- **D-22 — Watchlist lives in `UserRepository`.** The blueprint's three repos are
  Content/Playback/User; the watchlist (per-user library) is the natural fit for
  User. `ToggleWatchlistUseCase` → `UserRepository`.
- **D-23 — `savePosition` takes a `contentType`.** The blueprint's signature omits
  it, but `PlaybackPositionEntity.contentType` is non-null; the player knows the
  type, so it's a parameter.
- **D-24 — `ContentCacheModel` (@Serializable) mirrors `Content`** so the tab cache
  can be JSON-serialized while keeping the domain model annotation-free (§6).
- **D-25 — Added `EntityMapper.kt`** (entity → domain; not in §6.1) and extra DTO
  mappers `SubjectDetail.toDomain()` / `EpisodeInfo.toDomain()` needed by detail +
  episodes.
- **D-26 — Domain use cases use `@Inject` constructors in the processor-free
  `core:domain`.** Verified that Dagger generates their factories at the `:app`
  component site (temporary `@EntryPoint`, see Status). So no per-module Dagger
  processor is required in `core:domain`.
- **D-27 — `RecentSearchDao.upsert` is `@Insert(REPLACE)`** (dedup on the unique
  `query` index, since `@Upsert` keys on the PK). Added `WatchlistDao.existsOnce`
  for the toggle.
- **D-28 — `Room.databaseBuilder(...).fallbackToDestructiveMigration()`** for early
  development (no migrations until the schema stabilizes).

---

## 4. Known gaps carried forward

| Gap | Impact | Resolve in |
|---|---|---|
| **No search endpoint** in `ApiService` (§8.1) | `searchContent` emits empty | Phase 8 — add the endpoint when the API path is known |
| **Continue-Watching cards lack title/poster** | `PlaybackPositionEntity` stores no display fields, so `toContentStub()` leaves them empty | Phase 5/6 — enrich (extend the entity, or join with cached content) |
| `getContinueWatchingList` is a one-shot Flow | Won't live-update on new positions | Optional — switch the DAO to a `Flow` query |
| Clock-skew refresh / `signKeyVersion` check | from Phase 1 | a config-load step |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :core:data:assembleDebug      # Room KSP validates all DAO SQL
./gradlew :app:assembleDebug            # whole graph links
./gradlew :core:network:testDebugUnitTest   # Phase 1 signing test still green
```

### Phase 2 "done" checklist
- [x] `:core:domain` + `:core:data` compile. _(verified 2026-06-30)_
- [x] Room validates every DAO query and exports the schema.
- [x] Hilt resolves use case → repository → DAO/`ApiService` end-to-end.
- [x] `:app:assembleDebug` produces an APK.

---

## 6. Next: Phase 3 — Theme & Shared UI Components

Tasks **T-030 … T-036** in `:core:ui`: `ZinemaTheme`/`Color`/`Type`/`Shape`
(§10), then `ContentCard`, `WideContentCard`, `ShortCard`, `ContentRail`,
`HeroBanner`, `ShimmerRail`, and the small components (RatingBadge, GenreChip,
ErrorBanner, OfflineBanner). `:core:ui` already depends on `:core:domain`, so the
components can take `Content` directly.

---

*Generated as part of the Phase 2 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §6/§7 when starting Phase 3.*
