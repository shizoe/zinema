# Phase 6 — Detail Feature (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:core:data`,
> `:feature:detail`, and `:app:assembleDebug` all pass (no warnings); the app build
> validates `DetailViewModel`'s Hilt graph (`SavedStateHandle` + detail/episodes/
> watchlist use cases + `UserRepository`/`PlaybackRepository`).
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §11 → "Phase 6 — Detail Feature"
> (tasks **T-044, T-045, T-046**).

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-044** | `DetailViewModel` | ✅ | [DetailViewModel.kt](../feature/detail/src/main/kotlin/com/zinema/app/feature/detail/DetailViewModel.kt) |
| **T-045** | `DetailScreen` | ✅ | [DetailScreen.kt](../feature/detail/src/main/kotlin/com/zinema/app/feature/detail/DetailScreen.kt) |
| **T-046** | `EpisodeListSection` | ✅ | [EpisodeListSection.kt](../feature/detail/src/main/kotlin/com/zinema/app/feature/detail/EpisodeListSection.kt) |

Supporting (cross-layer): new `ContentDetail` domain model; `ContentRepository`
gains a richer `getContentDetail` + `getEpisodes`; new `GetEpisodesUseCase`;
`SubjectDetail.toContentDetail()` mapper.

---

## 2. Design notes

- **Richer detail model.** The lean `Content` (§6) has no episodes/seasons/related,
  so `getContentDetail` now returns a `ContentDetail` (content + seasons + episodes
  + related), built from a **single** `subject-api/get` call. Season switching uses
  `getEpisodes(subjectId, season)` (another call with the `se` param).
- **ViewModel.** `DetailViewModel` reads `subjectId` from `SavedStateHandle`, loads
  detail on init, and exposes: `uiState`, `isInWatchlist` (reactive `Flow` → state),
  `resume` (`PlaybackRepository.getPosition`), `selectedSeason`, and `episodes`.
  `toggleWatchlist()` and `selectSeason()` drive updates.
- **Collapsing backdrop.** Approximated with a `verticalScroll` column + a
  `graphicsLayer` that fades and parallaxes the backdrop as you scroll (rather than
  a full `CollapsingToolbarLayout`/`NestedScrollConnection`).
- **Conditional episodes.** For TV/anime, `EpisodeListSection` shows a season
  `ExposedDropdownMenuBox` (only when >1 season) and episode rows (thumbnail +
  title + duration), with a progress bar on the in-progress episode.
- **Play target.** Resume position wins; otherwise series start at the selected
  season's first episode and movies/shorts/sports play at (0, 0).
- **More Like This.** `ContentDetail.related` → the shared `ContentRail`.

---

## 3. Deviations & decisions (continuing from D-42)

- **D-43 — `getContentDetail` return type changed `Flow<Content>` → `Flow<ContentDetail>`**
  (+ new `ContentDetail` model, `getEpisodes`, `GetEpisodesUseCase`,
  `toContentDetail()` mapper). Necessary because the detail screen needs
  episodes/seasons/related that `Content` doesn't carry. Safe: nothing consumed the
  old signature yet.
- **D-44 — Collapsing toolbar is approximated** with `verticalScroll` +
  `graphicsLayer` (alpha + parallax), not a literal `NestedScrollConnection`-based
  collapsing layout. Same visual intent, far less machinery.
- **D-45 — `EpisodeListSection` renders a plain `Column`, not a `LazyColumn`** (T-046
  says "Episode LazyColumn"). A `LazyColumn` nested in the detail's `verticalScroll`
  would crash (infinite height); season episode counts are small.
- **D-46 — `DetailViewModel` injects `UserRepository` + `PlaybackRepository` directly**
  (for `isInWatchlist` / `getPosition`) alongside the use cases — there are no
  IsInWatchlist/GetPosition use cases in the task list.
- **D-47 — Per-episode progress shows only on the single resume episode.**
  `PlaybackPositionEntity` is keyed per **subject** (OQ-07), not per episode, so only
  one episode can reflect progress.
- **D-48 — `TrailerPreview.kt` (in the §4 file tree) was not created** — it's not in
  T-044–T-046. Trailer playback can be added later.

---

## 4. Known gaps carried forward

| Gap | Impact | Resolve in |
|---|---|---|
| Trailer preview (autoplay on detail) | not implemented | later (§4 `TrailerPreview`) |
| Per-episode resume progress | only the subject-level resume episode shows a bar | needs per-episode position keys |
| Share action | `onShareClick(content)` callback only — no `Intent` yet | nav/host wiring |
| Nav arg + routing into Detail/Player | screens take callbacks; no NavHost yet | `navigation/` (later) |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :core:data:assembleDebug
./gradlew :feature:detail:assembleDebug
./gradlew :app:assembleDebug     # validates DetailViewModel Hilt graph
```

### Phase 6 "done" checklist
- [x] `:feature:detail` compiles (ViewModel + screen + episode section). _(2026-06-30)_
- [x] `core:data` compiles with the `ContentDetail` change (Room SQL still valid).
- [x] `:app:assembleDebug` passes — `DetailViewModel` resolves via Hilt.
- [ ] Visual check on device (deferred until a NavHost passes `subjectId`).

---

## 6. Next: Phase 7 — Player Feature

Tasks **T-047 … T-052** in `:feature:player`: `PlayerViewModel` (`GetStreamInfoUseCase`,
CloudFront cookie injection, position reporting every 5s via
`SavePlaybackPositionUseCase`, expiry refresh, `StreamSecurityException` handling),
`PlayerScreen`/`TvPlayerScreen` (ExoPlayer by `StreamProtocol`), `PlayerControls`,
`SubtitleTrackSelector`, `QualitySelector`. `:feature:player` already depends on
`:core:network` (cookie jar) and pulls in Media3.

---

*Generated as part of the Phase 6 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §9 when starting Phase 7.*
