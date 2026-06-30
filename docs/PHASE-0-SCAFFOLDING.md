# Phase 0 — Project Scaffolding (Implementation Guide)

> **Status:** ✅ Complete and **build-verified**. On 2026-06-30 the project synced
> in Android Studio and `./gradlew :app:assembleDebug` produced a debug APK under
> JDK 17 + Gradle 8.9 (SDK platform android-35). The Gradle wrapper (`gradlew`,
> `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is now generated and present.
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §11 → "Phase 0 — Project Scaffolding"
> (tasks **T-001 … T-007**).

This document records exactly what was created, why, every deviation from the
blueprint, and how to get the project building. It is the hand-off guide into
Phase 1.

---

## 1. Task completion map

| Task | Description | Status | Artifacts |
|---|---|---|---|
| **T-001** | Root project + `settings.gradle.kts` listing all modules | ✅ | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` |
| **T-002** | `gradle/libs.versions.toml` with all deps from §2.2 | ✅ | `gradle/libs.versions.toml` |
| **T-003** | `build.gradle.kts` for each module per the §3.1 graph | ✅ | 12 module build files (see §3) |
| **T-004** | `google-services.json` placeholder + Crashlytics plugin | ✅ | `app/google-services.json`; `google-services` + `crashlytics` plugins applied in `app/build.gradle.kts` |
| **T-005** | `:app` `AndroidManifest.xml` (label, perms, RTL off, splash theme, leanback, TV banner) | ✅ | `app/src/main/AndroidManifest.xml` (+ resources in §5) |
| **T-006** | `proguard-rules.pro` (Serializable, Hilt, log strip, key protection) | ✅ | `app/proguard-rules.pro` + per-library `consumer-rules.pro` |
| **T-007** | `.gitignore`, `local.properties`, `google-services.json.example` | ✅ | `.gitignore`, `local.properties`, `app/google-services.json.example` |

---

## 2. Full file inventory

```
zinema/
├── settings.gradle.kts                 # T-001 — module list + repositories
├── build.gradle.kts                    # T-001 — root plugins (apply false)
├── gradle.properties                   # T-001 — AndroidX, parallel, caching
├── local.properties                    # T-007 — sdk.dir (git-ignored)
├── .gitignore                          # T-007
├── gradle/
│   ├── libs.versions.toml              # T-002 — version catalog
│   └── wrapper/gradle-wrapper.properties  # Gradle 8.9 pin
├── app/
│   ├── build.gradle.kts                # T-003/T-004 — application module
│   ├── google-services.json            # T-004 — PLACEHOLDER (fake keys)
│   ├── google-services.json.example    # T-007 — real-file template
│   ├── proguard-rules.pro              # T-006
│   └── src/main/
│       ├── AndroidManifest.xml         # T-005
│       ├── kotlin/com/zinema/app/
│       │   ├── ZinemaApplication.kt    # @HiltAndroidApp (stub)
│       │   └── MainActivity.kt         # platform detector (placeholder UI)
│       └── res/
│           ├── values/{strings,colors,themes}.xml
│           └── drawable/{ic_launcher,tv_banner}.xml
├── core/
│   ├── domain/build.gradle.kts         # pure Kotlin/JVM
│   ├── security/build.gradle.kts       # android lib
│   ├── network/build.gradle.kts        # android lib → core:security
│   ├── data/build.gradle.kts           # android lib → domain/network/security
│   └── ui/build.gradle.kts             # android lib (compose) → core:domain
└── feature/
    ├── auth|home|detail|search/build.gradle.kts   # android lib (compose)
    ├── player/build.gradle.kts         # + Media3
    └── shorttv/build.gradle.kts        # + Media3
```

Each Android library module also has a `consumer-rules.pro` (required because the
build files declare `consumerProguardFiles`).

---

## 3. Module graph (as implemented)

12 Gradle modules (see [§6 Deviation D-1](#6-deviations--decisions) re: the
blueprint's "9 modules" wording).

```
:app  (com.android.application)
 ├── :feature:auth ─┐
 ├── :feature:home  │
 ├── :feature:detail│  each → :core:domain, :core:ui
 ├── :feature:search│  (player + shorttv also → :core:network for streaming)
 ├── :feature:player┘
 ├── :feature:shorttv
 ├── :core:data     → :core:domain, :core:network, :core:security
 ├── :core:network  → :core:security
 ├── :core:security
 ├── :core:ui       → :core:domain
 └── :core:domain   (pure Kotlin/JVM, no deps)
```

| Module | Plugin type | Compose | Hilt | Notable libs |
|---|---|:--:|:--:|---|
| `:core:domain` | `kotlin.jvm` | — | — | coroutines-core, javax.inject |
| `:core:security` | android library | — | ✓ | security-crypto |
| `:core:network` | android library | — | ✓ | retrofit, okhttp, kotlinx-serialization, coil |
| `:core:data` | android library | — | ✓ | room, datastore, kotlinx-serialization |
| `:core:ui` | android library | ✓ | — | compose BOM, tv-material, coil, accompanist |
| `:feature:*` | android library | ✓ | ✓ | compose, navigation, hilt-navigation-compose |
| `:feature:player`/`:shorttv` | android library | ✓ | ✓ | + Media3 bundle |
| `:app` | android application | ✓ | ✓ | all features + all core + firebase + splash |

---

## 4. Key configuration values (pinned)

| Setting | Value | Where |
|---|---|---|
| `compileSdk` / `targetSdk` | 35 | every android module |
| `minSdk` | 26 | every android module |
| `applicationId` / namespace root | `com.zinema.app` | `app/build.gradle.kts` |
| Java / Kotlin JVM target | 17 | all modules |
| Gradle | 8.9 | `gradle-wrapper.properties` |
| AGP | 8.7.3 | catalog |
| Kotlin | 2.0.21 | catalog |
| Compose BOM | 2024.12.01 | catalog |
| versionName / versionCode | 1.0.0 / 1 | `app/build.gradle.kts` |

`BuildConfig.GUEST_JWT` is wired as a build-time field, sourced from the Gradle
property `ZINEMA_GUEST_JWT` (defaults to `""`). Set it in
`~/.gradle/gradle.properties` or pass `-PZINEMA_GUEST_JWT=...`.

---

## 5. `:app` manifest & resources (T-005)

`AndroidManifest.xml` provides:
- `INTERNET` + `ACCESS_NETWORK_STATE` permissions.
- `android:supportsRtl="false"`.
- `android:label="@string/app_name"` → **"Zinema"**.
- `android:banner="@drawable/tv_banner"` for the Android TV launcher.
- `<uses-feature leanback required=false>` and `touchscreen required=false` so the
  **single APK** installs on phones and TV.
- `MainActivity` exported with a launcher intent-filter carrying **both**
  `LAUNCHER` and `LEANBACK_LAUNCHER` categories, `supportsPictureInPicture`, and a
  `configChanges`/`uiMode` set suitable for the player + TV.

Supporting resources created so the manifest resolves and the project builds:
- `values/strings.xml` — `app_name`, `tagline` (this is blueprint **T-038b**,
  pulled forward so `@string/app_name` resolves now).
- `values/colors.xml` — Netflix-black window background.
- `values/themes.xml` — `Theme.Zinema` (base) + `Theme.Zinema.Starting` (splash).
- `drawable/ic_launcher.xml`, `drawable/tv_banner.xml` — placeholder vector art.

---

## 6. Deviations & decisions

The blueprint says *"Do not deviate without explicit instruction."* The items below
are **necessary completions** (things the blueprint references but doesn't fully
specify) rather than design changes. Each is called out so Phase 1+ can revisit.

- **D-1 — Module count "9" → 12.** T-001 and the Final Checklist say "9 modules",
  but §3 and §4 enumerate **12** (`app` + 5 `core` + 6 `feature`). The structure
  (§3/§4) is authoritative, so all 12 were created. The "9" appears to be stale.

- **D-2 — `:core:domain` is a pure Kotlin/JVM module.** The blueprint calls it
  "pure Kotlin (no Android imports)". To *enforce* that, it uses the
  `org.jetbrains.kotlin.jvm` plugin (not `com.android.library`). This required three
  catalog additions: the `kotlin-jvm` plugin, `coroutines-core` (JVM artifact, for
  `Flow` in repository interfaces), and `javax.inject` (for `@Inject` on use-case
  constructors). If you prefer zero catalog additions, convert it to an android
  library instead — but then "pure Kotlin" is only a convention.

- **D-3 — `:core:ui` depends on `:core:domain`.** §3.1's graph says core:ui has
  "no internal deps", but the component signatures in §10.3 / T-031 / T-033 are
  typed against the domain model `Content`. The component signatures win, so the
  dependency was added.

- **D-4 — `:app` depends on `:core:data/network/security` directly.** §3.1 draws
  `app → feature:*` only. But the feature modules depend on `domain`/`ui`, not on
  the impl modules — so Hilt would never see `NetworkModule`, `DataModule`, or
  `SecurityModule`. The app must have those modules on its classpath for Hilt
  component generation, so explicit deps were added.

- **D-5 — Splash theme naming.** T-005 literally says
  `android:theme="@style/Theme.SplashScreen"`, but redefining that id would clash
  with the `core-splashscreen` library style. Instead the activity uses
  `Theme.Zinema.Starting` (which `parent="Theme.SplashScreen"` and sets
  `postSplashScreenTheme`), the standard, correct setup.

- **D-6 — Stub `ZinemaApplication` + `MainActivity`.** A manifest that names
  non-existent classes won't build. Minimal stubs were added so the scaffold
  compiles and runs: `ZinemaApplication` is the real `@HiltAndroidApp`;
  `MainActivity` keeps the §9.1 platform-detection logic but renders a placeholder
  instead of `TvNavigation()/AppNavigation()` (which don't exist until Phase 4/5).
  Both are marked with `TODO(Phase …)`.

- **D-7 — Catalog conveniences.** Added `[bundles]` (`compose`, `tv`, `media3`) to
  keep the 8 compose module files terse and consistent. Pure additive.

- **D-8 — KSP everywhere.** Hilt and Room both use KSP (not kapt). The `kotlin-kapt`
  plugin remains declared in the catalog but unused.

- **D-9 — `google-services.json` is committed as a non-functional placeholder** so
  the `google-services` plugin configures and the project builds out-of-the-box.
  Replace it with the real Firebase file (see `google-services.json.example`). The
  `.gitignore` has a commented line to start ignoring it once it holds real keys.

---

## 7. Build & verify

This was **not** built here (no Android SDK; JDK 8 only). To verify Phase 0:

1. **Open in Android Studio** (Ladybug / 2024.2+ recommended for AGP 8.7).
   - Ensure an embedded/installed **JDK 17** is selected as the Gradle JDK.
   - Confirm `local.properties → sdk.dir` points at your SDK (Android SDK with
     **API 35** installed).
2. **Gradle wrapper** — already generated (`gradlew`, `gradlew.bat`,
   `gradle/wrapper/gradle-wrapper.jar`, pinned to 8.9). Use `./gradlew` directly.
3. **Sync & assemble:**
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. Optional sanity tasks:
   ```bash
   ./gradlew projects            # should list all 12 modules
   ./gradlew :app:dependencies   # confirm the module graph resolves
   ```

### Phase 0 "done" checklist
- [x] `./gradlew projects` lists all 12 modules.
- [x] `:app:assembleDebug` produces an APK. _(verified 2026-06-30)_
- [ ] App launches to the "Mobile/TV scaffold ready" placeholder.
- [ ] Installs on an Android TV emulator and shows in the TV launcher (banner).
- [ ] Replace placeholder `google-services.json` before shipping analytics.

---

## 8. Known gaps carried into later phases

| Gap | Resolved in |
|---|---|
| Real `MainActivity` navigation + `ZinemaTheme` | Phase 3 (T-030), Phase 4/5 |
| `network_security_config.xml` (manifest `TODO`) | Phase 10 (T-062) |
| Signing-key hardening beyond ProGuard (JNI/.so) | Phase 10 (T-063) |
| Gradle wrapper **jar** (binary) | ✅ Done — generated 2026-06-30 |
| Real Firebase config | Drop-in before release |
| `GUEST_JWT` value | Phase 4 (T-037) — set the Gradle property |

---

## 9. Next: Phase 1 — Core Security & Network

Tasks **T-008 … T-019**, landing in `:core:security` and `:core:network`:
`TokenStorage`, `DeviceIdProvider`, `SecurityModule`, the four interceptors
(`Signing`, `Auth`, `ClientInfo`, `LogScrub`), `CloudFrontCookieJar`,
`CdnValidator`, all DTOs (§5), `ApiService` (§8.1), and `NetworkModule` (§8.7) —
plus the HMAC unit test (T-011). The build files for both modules already declare
every dependency those tasks need.

---

*Generated as part of the Phase 0 implementation pass. Pair this with
`ANDROID_BLUEPRINT.md` §11 when starting Phase 1.*
