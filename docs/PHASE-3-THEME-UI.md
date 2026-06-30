# Phase 3 — Theme & Shared UI Components (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:core:ui:assembleDebug`
> and `:app:assembleDebug` both pass under JDK 17 / Gradle 8.9.
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §10 and §11 → "Phase 3 — Theme &
> Shared UI Components" (tasks **T-030 … T-036**). All in `:core:ui`.

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-030** | Theme: ZinemaTheme/Color/Type/Shape | ✅ | `core/ui/theme/` — [ZinemaTheme.kt](../core/ui/src/main/kotlin/com/zinema/app/core/ui/theme/ZinemaTheme.kt), Color/Type/Shape |
| **T-031** | `ContentCard` (2:3 poster) | ✅ | [ContentCard.kt](../core/ui/src/main/kotlin/com/zinema/app/core/ui/components/ContentCard.kt) |
| **T-032** | `WideContentCard` (16:9), `ShortCard` (9:16) | ✅ | WideContentCard.kt, ShortCard.kt |
| **T-033** | `ContentRail` | ✅ | [ContentRail.kt](../core/ui/src/main/kotlin/com/zinema/app/core/ui/components/ContentRail.kt) |
| **T-034** | `HeroBanner` | ✅ | [HeroBanner.kt](../core/ui/src/main/kotlin/com/zinema/app/core/ui/components/HeroBanner.kt) |
| **T-035** | `ShimmerRail` | ✅ | [ShimmerRail.kt](../core/ui/src/main/kotlin/com/zinema/app/core/ui/components/ShimmerRail.kt) |
| **T-036** | RatingBadge, GenreChip, ErrorBanner, OfflineBanner | ✅ | `core/ui/components/` |

Support files: `util/Platform.kt` (`isTvDevice()` + `LocalIsTv`) and
`util/ModifierExtensions.kt` (`Modifier.shimmerBackground()`).

---

## 2. Design notes

- **Dual theme (T-030).** `ZinemaTheme` nests **both** the Material3 theme (mobile
  chrome) and the tv-material theme (TV chrome) over the same brand palette, and
  publishes `LocalIsTv` (resolved once from `UiModeManager`). Shared components read
  `LocalIsTv` to pick Material3 vs tv-material widgets.
- **Cards.** `ContentCard`/`WideContentCard`/`ShortCard` each branch on `LocalIsTv`:
  TV → `androidx.tv.material3.Card` (built-in D-pad focus scaling, so no manual
  1.08 scale needed); mobile → `androidx.compose.material3.Card`. Imports are
  aliased (`M3Card` / `TvCard`) to avoid the name clash. tv-material APIs are opted
  into with `@OptIn(ExperimentalTvMaterial3Api::class)`.
- **Images.** Cards use Coil `AsyncImage` over a `shimmerBackground()` placeholder
  that shows through until the bitmap loads. (See [§4](#4-known-gaps) re: which
  `ImageLoader` is used.)
- **HeroBanner.** Foundation `HorizontalPager` + `rememberPagerState`, auto-advances
  every 5s via `LaunchedEffect`, 56% screen height, `Transparent → Background`
  gradient, Play / + My List buttons, and indicator dots (active white / inactive
  40%).
- **Icons.** Deliberately avoided the material-icons dependency — the star and
  play/plus glyphs are Unicode (`★`, `▶`, `+`) so `:core:ui` stays lean.

---

## 3. Deviations & decisions (continuing from D-28)

- **D-29 — Hover/focus 1.08 scale (§10.3) is delegated to tv-material on TV** (its
  `Card` scales on focus by default) and omitted on mobile (touch has no hover). A
  custom scale can be layered later if desired.
- **D-30 — `ImageUrlValidator.kt` (listed in §4 tree) was not created in `:core:ui`.**
  It would duplicate `CdnValidator`, and `:core:ui` can't depend on `:core:network`.
  The image-host allowlist is already enforced in the network `ImageLoader`
  (`NetworkModule`, Phase 1).
- **D-31 — Shimmer lives in `Modifier.shimmerBackground()`** (`util/`) and is reused
  by `ShimmerRail` and every card placeholder, rather than a one-off in ShimmerRail.

---

## 4. Known gaps carried forward

| Gap | Impact | Resolve in |
|---|---|---|
| `AsyncImage` uses Coil's **default** singleton `ImageLoader`, not the Hilt-provided one | The CDN allowlist interceptor + 200MB disk cache aren't applied to images yet | App wiring: make `ZinemaApplication` implement `coil.ImageLoaderFactory` returning the injected `ImageLoader` (small, do in Phase 5 or 10) |
| `HeroBanner` is mobile-oriented | TV hero focus/D-pad handling | Phase 5 `TvHomeScreen` (T-043) |
| Components are unpreviewed | No `@Preview` smoke screens | optional |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :core:ui:assembleDebug
./gradlew :app:assembleDebug
```

### Phase 3 "done" checklist
- [x] `:core:ui` compiles (theme + 10 components + 2 util files). _(verified 2026-06-30)_
- [x] tv-material / Material3 dual-theme + `LocalIsTv` branching compiles.
- [x] `:app:assembleDebug` still produces an APK.
- [ ] Visual check on device/emulator (deferred until a screen hosts these — Phase 5).

---

## 6. Next: Phase 4 — Auth Feature

Tasks **T-037 … T-039b** in `:feature:auth`: `AuthViewModel` (guest login via
`BuildConfig.GUEST_JWT`, token-expiry collection), `LoginScreen`, `strings.xml`
(app_name/tagline — already added in Phase 0), and `ProfileSelectorScreen`. This is
the first feature ViewModel, so it exercises the Phase 2 Hilt graph for real
(already validated via the temporary entry point).

---

*Generated as part of the Phase 3 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §10 when starting Phase 4.*
