# Phase 5 — Home & Browse Feature (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:feature:home:assembleDebug`
> and `:app:assembleDebug` pass; the app build validates `HomeViewModel`
> (`ToggleWatchlistUseCase`) and `TabContentViewModel` (`GetTabContentUseCase`) Hilt
> graphs, plus the new Coil `ImageLoader` injection into `ZinemaApplication`.
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §9.3, §2.4 (tab table) and §11 →
> "Phase 5 — Home & Browse Feature" (tasks **T-040 … T-043**).

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-040** | `HomeViewModel` + `TabContentViewModel` | ✅ | [HomeViewModel.kt](../feature/home/src/main/kotlin/com/zinema/app/feature/home/HomeViewModel.kt), [TabContentViewModel.kt](../feature/home/src/main/kotlin/com/zinema/app/feature/home/TabContentViewModel.kt) |
| **T-041** | `CONTENT_TABS` (PRD §2.4, drop tabId=27) | ✅ | [ContentTabs.kt](../feature/home/src/main/kotlin/com/zinema/app/feature/home/ContentTabs.kt) |
| **T-042** | `HomeScreen` (mobile) | ✅ | [HomeScreen.kt](../feature/home/src/main/kotlin/com/zinema/app/feature/home/HomeScreen.kt) + [HomeContent.kt](../feature/home/src/main/kotlin/com/zinema/app/feature/home/HomeContent.kt) |
| **T-043** | `TvHomeScreen` (TV) | ✅ | [TvHomeScreen.kt](../feature/home/src/main/kotlin/com/zinema/app/feature/home/TvHomeScreen.kt) |

Also: `ZinemaApplication` now implements `coil.ImageLoaderFactory` (resolves the
Phase 3 image-loader gap).

---

## 2. Design notes

- **Tabs.** `CONTENT_TABS` is the full 15-tab PRD §2.4 list; `HomeViewModel`
  exposes the visible set (filtering `SUPPRESSED_TAB_IDS = {27}`, OQ-06). Bottom nav
  shows `BOTTOM_NAV_TAB_IDS` (Home, Movies, TV, Trending, ShortTV) + a **More** item
  that opens a full-screen category grid of all tabs.
- **Two ViewModels (§9.3).** `HomeViewModel` owns tab selection + the hero "+ My
  List" action; `TabContentViewModel` loads the selected tab's feed and exposes
  `TabContentUiState` (Loading / Success / Empty / Error). `HomeScreen` calls
  `loadTab(selectedTabId)` from a `LaunchedEffect`.
- **Rails from a flat list.** The repository returns a flat, deduped `List<Content>`
  (T-026), so `TabContentViewModel` derives rails by **grouping on `ContentType`**
  ("Movies", "TV Shows", "Anime", …) and uses the first 5 items as the hero
  (PHASE-5 §Deviations). `HomeContent` renders hero + rails in a `LazyColumn`.
- **States.** Loading → 4 `ShimmerRail`s; Empty → the geo-block message (OQ-08);
  Error → `ErrorBanner` with retry.
- **TV.** `TvHomeScreen` is a `Row` of a left nav rail (all tabs, selected
  highlighted) + the shared `HomeContent`.
- **Images.** `ZinemaApplication : ImageLoaderFactory` returns the Hilt
  `ImageLoader`, so every `AsyncImage` now uses the CDN-allowlisted client + 200MB
  disk cache.

---

## 3. Deviations & decisions (continuing from D-36)

- **D-37 — Rails are grouped by `ContentType`, hero = first 5.** §9.3 shows
  `Success(rails)`, but T-026's repository flattens blocks into one `List<Content>`,
  so block→rail structure is lost. Grouping by type yields meaningful named rails
  from available data. (A future option: have the repo preserve block structure.)
- **D-38 — Both `HomeViewModel` and `TabContentViewModel` exist** (per the §4 file
  list); selection lives in one, content loading in the other.
- **D-39 — Tab/nav icons are Unicode glyphs** (no material-icons dependency,
  consistent with Phase 3). PRD §2.4 icon names are approximated.
- **D-40 — `TvHomeScreen` uses standard Compose `LazyColumn` + clickable nav rows**,
  not tv-foundation's `TvLazyColumn`/`rememberTvLazyListState` (deprecated APIs).
  Card focus scaling still comes from tv-material via `ContentCard`. Deeper D-pad
  focus management is deferred.
- **D-41 — `OfflineBanner` is not shown yet** — there's no `ConnectivityObserver`
  until T-060 (Phase 10). The component is ready to drop in.
- **D-42 — Coil `ImageLoaderFactory` wired now** in `ZinemaApplication` (the Phase 3
  follow-up), so the allowlisted `ImageLoader` is actually used.

---

## 4. Known gaps carried forward

| Gap | Impact | Resolve in |
|---|---|---|
| Pagination / "Load More" (F-02.1.5, max 5 pages) | only page 1 is loaded | Home enhancement |
| `ConnectivityObserver` + OfflineBanner | offline state not surfaced | Phase 10 (T-060) |
| ShortTV bottom-nav loads as a normal tab | should open the vertical swipe feed | Phase 9 (nav routes to `ShortTvScreen`) |
| `tab_viewed` / `content_impressed` analytics | not instrumented | Phase 10 (T-058) |
| Real multi-rail structure from server blocks | rails are type-grouped, not block-named | repo change if desired |
| Continue Watching rail | not shown on Home | needs enrichment (Phase 2 gap) + a rail |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :feature:home:assembleDebug
./gradlew :app:assembleDebug
```

### Phase 5 "done" checklist
- [x] `:feature:home` compiles (2 ViewModels + 3 screen files + tab config). _(2026-06-30)_
- [x] `:app:assembleDebug` passes — both home ViewModels resolve via Hilt.
- [x] Coil uses the Hilt `ImageLoader` (CDN allowlist + disk cache).
- [ ] Visual check on device/emulator (deferred until a NavHost hosts Home).

---

## 6. Next: Phase 6 — Detail Feature

Tasks **T-044 … T-046** in `:feature:detail`: `DetailViewModel`
(`GetContentDetailUseCase`, watchlist state via `UserRepository.isInWatchlist` +
`ToggleWatchlistUseCase`, `PlaybackRepository.getPosition`), `DetailScreen`
(collapsing backdrop, Play, + My List, genre chips, expandable synopsis,
conditional season/episode list, "More Like This" rail), and `EpisodeListSection`.
The Phase 2 use cases + Phase 3 components are ready.

---

*Generated as part of the Phase 5 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §9 when starting Phase 6.*
