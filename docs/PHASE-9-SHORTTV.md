# Phase 9 — ShortTV Feature (Mobile Only) — Implementation Guide

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:feature:shorttv` and
> `:app:assembleDebug` pass; the app build validates `ShortTvViewModel`'s Hilt graph
> (`GetTabContentUseCase` + `GetStreamInfoUseCase` + `CloudFrontCookieJar` + shared
> `OkHttpClient`).
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §11 → "Phase 9 — ShortTV Feature
> (Mobile Only)" (tasks **T-055, T-056**).

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-055** | `ShortTvViewModel` | ✅ | [ShortTvViewModel.kt](../feature/shorttv/src/main/kotlin/com/zinema/app/feature/shorttv/ShortTvViewModel.kt) |
| **T-056** | `ShortTvScreen` | ✅ | [ShortTvScreen.kt](../feature/shorttv/src/main/kotlin/com/zinema/app/feature/shorttv/ShortTvScreen.kt) |

---

## 2. Design notes

- **Feed + single player.** `ShortTvViewModel` loads `tabId=13` and owns one
  **muted, looping** `ExoPlayer` (built on the shared `OkHttpClient`, so the
  CloudFront cookie jar applies). `ShortTvScreen` is a full-screen `VerticalPager`;
  only the **current page** renders a `PlayerView` bound to that shared player
  (TikTok-style), so memory stays flat regardless of feed length.
- **On-the-fly stream resolution.** When the visible page changes,
  `onPageVisible(index)` resolves that item's stream via `GetStreamInfoUseCase`,
  injects its `play_auth` cookies, and starts playback; it also **preloads the next
  two** items' stream URLs.
- **Interactions.** Tap toggles mute (`player.volume`); a "+ Watch Series" button
  fades in after 2s; the poster shows underneath until the surface renders.
- **Lifecycle.** The player is released in `onCleared`.

---

## 3. Deviations & decisions (continuing from D-61)

- **D-62 — `VerticalPager` instead of `LazyColumn` + `rememberSnapFlingBehavior`**
  (T-056). Same full-screen vertical snap UX, and `pagerState.currentPage` gives the
  "current visible item index" for free (T-055).
- **D-63 — One shared `ExoPlayer` rebound to the current page**, not a player per
  item. Efficient and the standard pattern for vertical video feeds; player is muted
  and `REPEAT_MODE_ONE`.
- **D-64 — Streams resolved via `GetStreamInfoUseCase` + cookie injection.** Shorts
  stream from `msacdn.*` and are signed, so the same security path as the main player
  applies. "Preload next 2" resolves their URLs ahead of time.
- **D-65 — `DefaultMediaSourceFactory` + `MediaItem.fromUri`** rather than an explicit
  `ProgressiveMediaSource` (T-056). The factory selects progressive for the `.mp4`
  short, consistent with the Phase 7 player.

---

## 4. Known gaps carried forward

| Gap | Resolve in |
|---|---|
| Preload = stream-URL resolution, not media buffering | a Media3 preload manager if needed |
| Fine-grained `VisibilityTracker` | currently uses `pagerState.currentPage` (sufficient) |
| Pause on app background / persist mute choice | small UX pass |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :feature:shorttv:assembleDebug
./gradlew :app:assembleDebug     # validates ShortTvViewModel Hilt graph
```

### Phase 9 "done" checklist
- [x] `:feature:shorttv` compiles (ViewModel + screen). _(2026-06-30)_
- [x] `:app:assembleDebug` passes — `ShortTvViewModel` resolves via Hilt.
- [ ] On-device vertical playback (deferred until nav + a valid token).

---

## 6. Next: Phase 10 — Analytics & Final Wiring (+ navigation)

Tasks **T-057 … T-063**: `AnalyticsTracker` (Firebase, PII-guarded), event
instrumentation, Crashlytics wiring, `ConnectivityObserver` (+ wire `OfflineBanner`),
the signing-interceptor smoke test, `network_security_config.xml`, and the ProGuard
key hardening.

> **Heads-up (not a numbered task, but required to actually run the app):** the
> `navigation/` graph (`Screen`, `AppNavigation`, `TvNavigation`) and the real
> `MainActivity` are still the Phase 0 placeholder. All feature screens are built to
> take navigation callbacks, so wiring a `NavHost` (mobile + TV) is the remaining
> integration step that turns the modules into a running app. Worth doing alongside
> Phase 10.

---

*Generated as part of the Phase 9 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §11 when starting Phase 10.*
