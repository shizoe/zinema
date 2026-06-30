# Phase 7 — Player Feature (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:core:data`,
> `:feature:player`, and `:app:assembleDebug` all pass; the app build validates
> `PlayerViewModel`'s Hilt graph (the `CloudFrontCookieJar` + shared `OkHttpClient`
> + `GetStreamInfoUseCase`/`SavePlaybackPositionUseCase` + `SubtitlePreferences`).
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §11 → "Phase 7 — Player Feature"
> (tasks **T-047 … T-052**).

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-047** | `PlayerViewModel` | ✅ | [PlayerViewModel.kt](../feature/player/src/main/kotlin/com/zinema/app/feature/player/PlayerViewModel.kt) |
| **T-048** | `PlayerScreen` (mobile) | ✅ | [PlayerScreen.kt](../feature/player/src/main/kotlin/com/zinema/app/feature/player/PlayerScreen.kt) |
| **T-049** | `TvPlayerScreen` (TV) | ✅ | [TvPlayerScreen.kt](../feature/player/src/main/kotlin/com/zinema/app/feature/player/TvPlayerScreen.kt) |
| **T-050** | `PlayerControls` + `TvPlayerControls` | ✅ | PlayerControls.kt, TvPlayerControls.kt |
| **T-051** | `SubtitleTrackSelector` (+ DataStore) | ✅ | SubtitleTrackSelector.kt, SubtitlePreferences.kt |
| **T-052** | `QualitySelector` | ✅ | QualitySelector.kt |

---

## 2. The streaming security path (why this matters)

The blueprint's whole point is that signed CDN cookies must reach the stream host
**without** leaking. This implementation keeps that intact:

1. `getStreamInfo` (repo) validates the host with `CdnValidator.isStreamHost` and
   throws `StreamSecurityException` otherwise (Phase 2).
2. `PlayerViewModel.injectCookies` sets the `CloudFront-*` triple on the
   `CloudFrontCookieJar` for the stream host only (the jar itself rejects other
   hosts).
3. The `ExoPlayer` is built with `OkHttpDataSource.Factory(okHttpClient)` — the
   **same** Hilt `OkHttpClient` that owns the cookie jar — so every segment request
   carries the cookies, and only allowlisted hosts are ever contacted.
4. Subtitle side-loads are filtered through `CdnValidator.isAllowed`.

`StreamSecurityException` → an error state (and a log; Crashlytics is wired in
Phase 10).

---

## 3. Design notes

- **ViewModel owns the player.** `PlayerViewModel` builds/holds the `ExoPlayer`,
  resolves the stream, reports position every **5s** via
  `SavePlaybackPositionUseCase`, refreshes ~60s before `expiresAt`, and releases the
  player in `onCleared`.
- **Media sources.** `DefaultMediaSourceFactory` + `MediaItem` mime-type hints
  (MPD/M3U8/none) pick DASH/HLS/Progressive and auto-merge `SubtitleConfiguration`s.
- **Quality.** `StreamInfo` now carries `availableQualities`; `QualitySelector` lists
  them and `changeQuality` re-resolves at the chosen quality (resuming position).
- **Subtitles.** `SubtitleTrackSelector` (Off + tracks) drives
  `player.trackSelectionParameters`; the choice is persisted in `SubtitlePreferences`
  (DataStore) and re-applied on load.
- **Mobile UX.** Forced `SENSOR_LANDSCAPE`, custom controls (auto-hide after 3s),
  tap-to-toggle, double-tap ±10s seek, and a PiP button.
- **TV UX.** D-pad-friendly focusable controls (±10s, play/pause, CC, quality) + a
  back affordance.

---

## 4. Deviations & decisions (continuing from D-48)

- **D-49 — `StreamInfo.availableQualities` + `getStreamInfo(quality)` added** (Phase 2
  evolution). The domain `StreamInfo` had only the chosen quality, but T-052 needs
  the full list and quality switching. Safe: nothing consumed the old shape.
- **D-50 — `PlayerViewModel` owns the `ExoPlayer`**, built on the shared
  `OkHttpClient` so the CloudFront cookie jar applies. Centralizes lifecycle (vs.
  building it in the composable).
- **D-51 — `DefaultMediaSourceFactory` + `MediaItem` mime/subtitle configs** instead
  of explicit `DashMediaSource`/`HlsMediaSource`/`SingleSampleMediaSource` (T-048).
  Same result, modern API, and subtitles auto-merge. Subtitle hosts gated by
  `CdnValidator`.
- **D-52 — Crashlytics call deferred.** `feature:player` has no Firebase dependency;
  `StreamSecurityException` is logged + surfaced. `Crashlytics.recordException` is a
  `TODO(Phase 10, T-059)`.
- **D-53 — PiP via a control button** (+ `supportsPictureInPicture` already in the
  manifest). Auto-PiP on `onUserLeaveHint` needs `MainActivity` wiring (deferred).
  Swipe volume/brightness is **not** implemented.
- **D-54 — `SubtitleTrackSelector` uses `ModalBottomSheet` on both platforms**
  (T-051 says Dialog on TV).
- **D-55 — TV "next episode" row deferred** — the player only receives ids, not the
  episode list.
- **D-56 — `contentType` for position saves** is read from an optional nav arg,
  defaulting to `MOVIE` (the player route carries no content type).
- **D-57 — `expiresAt` parsed as epoch millis** (the field's exact format is
  unspecified); unparseable → no refresh scheduled.

---

## 5. Known gaps carried forward

| Gap | Resolve in |
|---|---|
| 403-during-playback → silent credential refresh + resume (F-03.2.4) | player error-listener enhancement |
| Auto-PiP on home-button (`onUserLeaveHint`) | `MainActivity` wiring |
| Swipe volume / brightness gestures | player UX pass |
| TV next-episode row | pass episode list into the player route |
| `Crashlytics.recordException` for stream/playback errors | Phase 10 (T-059) |
| `contentType` in the player nav route | navigation wiring |

---

## 6. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :feature:player:assembleDebug
./gradlew :app:assembleDebug     # validates PlayerViewModel Hilt graph
```

### Phase 7 "done" checklist
- [x] `:feature:player` compiles (ViewModel + 2 screens + 2 controls + 2 selectors +
      DataStore). _(2026-06-30)_
- [x] `:app:assembleDebug` passes — `PlayerViewModel` resolves the cookie jar +
      shared `OkHttpClient` + use cases.
- [ ] On-device playback against a real stream (deferred until nav + a valid token).

---

## 7. Next: Phase 8 — Search Feature

Tasks **T-053, T-054** in `:feature:search`: `SearchViewModel` (debounced query →
`SearchContentUseCase`, recent searches via `RecentSearchDao`) and `SearchScreen`.
**Note:** `searchContent` currently returns empty — the search API endpoint is still
undefined (§8.1), so Phase 8 will build the UI + recents and leave the network call
stubbed until the endpoint is known.

---

*Generated as part of the Phase 7 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §11 when starting Phase 8.*
