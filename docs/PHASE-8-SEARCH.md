# Phase 8 — Search Feature (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:core:data`,
> `:feature:search`, and `:app:assembleDebug` pass; the app build validates
> `SearchViewModel`'s Hilt graph (`SearchContentUseCase` + `SearchHistoryRepository`).
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §11 → "Phase 8 — Search Feature"
> (tasks **T-053, T-054**).
>
> ⚠️ **Functional caveat:** results stay empty because the search **API endpoint is
> undefined** (`ApiService`, §8.1). The full pipeline (debounce → query → grouped
> results) and recent-searches behavior are complete and will light up the moment
> `ContentRepository.searchContent` is wired to a real endpoint.

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-053** | `SearchViewModel` | ✅ | [SearchViewModel.kt](../feature/search/src/main/kotlin/com/zinema/app/feature/search/SearchViewModel.kt) |
| **T-054** | `SearchScreen` (mobile + TV) | ✅ | [SearchScreen.kt](../feature/search/src/main/kotlin/com/zinema/app/feature/search/SearchScreen.kt) |

Supporting (new): `SearchHistoryRepository` (domain) + `SearchHistoryRepositoryImpl`
(data, bound in `DataModule`); `RecentSearchDao` gains `observeRecent` + `trimToLimit`.

---

## 2. Design notes

- **Debounced query → grouped results.** `SearchViewModel.results` is `query`
  `.debounce(300ms).distinctUntilChanged().flatMapLatest { searchContent(it) }`,
  emitting `Idle` / `Empty` / `Results(Map<ContentType, List<Content>>)`.
- **Recent searches (layered).** `recentSearches` comes from
  `SearchHistoryRepository` (a reactive `Flow<List<String>>`). `onQuerySubmit`
  records a query (dedup via the unique index) and trims to 10 (oldest evicted);
  `clearRecentSearches` wipes it.
- **Screen.** A styled search field at the top; when the query is empty it shows
  recent-search chips + a Clear action, otherwise grouped result rails (one
  `ContentRail` per `ContentType`) or a "no results" message.

---

## 3. Deviations & decisions (continuing from D-57)

- **D-58 — `SearchHistoryRepository` introduced** (domain interface + data impl) so
  `feature:search` reaches recent searches through the domain layer rather than
  injecting `RecentSearchDao` from `core:data` directly.
- **D-59 — `RecentSearchDao` gained `observeRecent` (Flow) + `trimToLimit(keep)`.**
  T-023 only specified `getRecent`/`deleteAll`; the reactive list and the
  "max 10, evict oldest" rule (T-053) need these.
- **D-60 — `SearchScreen` uses a styled `OutlinedTextField`, not the M3 `SearchBar`**
  (whose reworked `inputField` API is fiddly and which has no tv-material
  equivalent). The same screen serves mobile + TV. Results are grouped into
  `ContentRail`s by type.
- **D-61 — Results are inert until the search endpoint exists** (carryover of the
  Phase 2 gap). `searchContent` returns empty; everything downstream is ready.

---

## 4. Known gaps carried forward

| Gap | Resolve in |
|---|---|
| **Search API endpoint** (`searchContent` returns empty) | when the API path is known — wire `ContentRepositoryImpl.searchContent` |
| TV Leanback-style search bar | a TV-specific input if desired |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :core:data:assembleDebug      # Room validates observeRecent + trimToLimit
./gradlew :feature:search:assembleDebug
./gradlew :app:assembleDebug            # validates SearchViewModel Hilt graph
```

### Phase 8 "done" checklist
- [x] `:feature:search` compiles (ViewModel + screen). _(2026-06-30)_
- [x] Room validates the new recent-search queries (incl. the trim subquery).
- [x] `:app:assembleDebug` passes — `SearchViewModel` resolves via Hilt.
- [ ] Live results (blocked on the search endpoint).

---

## 6. Next: Phase 9 — ShortTV Feature (Mobile Only)

Tasks **T-055, T-056** in `:feature:shorttv`: `ShortTvViewModel` (loads tabId=13,
tracks the visible item, triggers stream load on viewport entry) and `ShortTvScreen`
(vertical snapping `LazyColumn`, full-height 9:16 items, muted autoplay ExoPlayer
per item with preload). `:feature:shorttv` already pulls in Media3 and depends on
`:core:network`.

---

*Generated as part of the Phase 8 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §11 when starting Phase 9.*
