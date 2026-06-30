# Phase 1 — Core Security & Network (Implementation Guide)

> **Status:** ✅ Complete (files generated). ⚠️ Not compiled here (no Android SDK;
> see Phase 0 doc). The signing HMAC, however, **was** validated against an
> independently-computed value using a local JDK — see [§4](#4-the-signing-test-t-011).
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §5, §8, and §11 → "Phase 1 — Core
> Security & Network" (tasks **T-008 … T-019**).

This phase implements everything in `:core:security` and `:core:network`: encrypted
token/device storage, the four OkHttp interceptors, the CloudFront cookie jar, the
CDN allowlist, all API DTOs, the Retrofit service, and the Hilt wiring.

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-008** | `TokenStorage` (EncryptedSharedPreferences) | ✅ | [TokenStorage.kt](../core/security/src/main/kotlin/com/zinema/app/core/security/TokenStorage.kt) |
| **T-009** | `DeviceIdProvider` (SHA-256(ANDROID_ID), cached) | ✅ | [DeviceIdProvider.kt](../core/security/src/main/kotlin/com/zinema/app/core/security/DeviceIdProvider.kt) |
| **T-010** | `SecurityModule` (Hilt, @Singleton) | ✅ | [SecurityModule.kt](../core/security/src/main/kotlin/com/zinema/app/core/security/SecurityModule.kt) |
| **T-011** | `SigningInterceptor` (x-tr-signature) + HMAC test | ✅ | [SigningInterceptor.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/interceptors/SigningInterceptor.kt), [SigningInterceptorTest.kt](../core/network/src/test/kotlin/com/zinema/app/core/network/SigningInterceptorTest.kt) |
| **T-012** | `AuthInterceptor` (Bearer + 401 signal) | ✅ | [AuthInterceptor.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/interceptors/AuthInterceptor.kt) |
| **T-013** | `ClientInfoInterceptor` (x-client-info + headers) | ✅ | [ClientInfoInterceptor.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/interceptors/ClientInfoInterceptor.kt) |
| **T-014** | `CloudFrontCookieJar` | ✅ | [CloudFrontCookieJar.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/cdn/CloudFrontCookieJar.kt) |
| **T-015** | `CdnValidator` (host allowlist) | ✅ | [CdnValidator.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/cdn/CdnValidator.kt) |
| **T-016** | `NetworkModule` (OkHttp/Retrofit/Coil) | ✅ | [NetworkModule.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/NetworkModule.kt) |
| **T-017** | All DTOs (§5) | ✅ | [dto/](../core/network/src/main/kotlin/com/zinema/app/core/network/dto) (5 files) |
| **T-018** | `ApiService` interface | ✅ | [ApiService.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/ApiService.kt) |
| **T-019** | `LogScrubInterceptor` (+ redactHeader wiring) | ✅ | [LogScrubInterceptor.kt](../core/network/src/main/kotlin/com/zinema/app/core/network/interceptors/LogScrubInterceptor.kt) |

Plus a supporting type: `SessionState` (kids-profile flags) in `:core:domain` —
see [§3 D-10](#3-deviations--decisions).

---

## 2. How the request pipeline fits together

Application-interceptor order (blueprint **T-016**), set in `NetworkModule`:

```
ClientInfoInterceptor  → adds x-client-info JSON + session headers, User-Agent, Accept
AuthInterceptor        → adds "Authorization: Bearer <jwt>", flags 401 → token expired
SigningInterceptor     → adds x-tr-signature (must run LAST: signs the final header set)
LogScrubInterceptor    → pass-through hook
HttpLoggingInterceptor → network interceptor; BASIC in debug / NONE in release,
                         with redactHeader() for every sensitive header
```

**Signing (`x-tr-signature`)** — `"<ts>|2|<HmacMD5_Base64(stringToSign)>"` where
`stringToSign` joins by `\n`: `METHOD, Accept, Content-Type, bodyLen, ts, bodyMd5,
sortedPathQuery`. `ts` is corrected for clock skew via
`TokenStorage.getServerTimeOffsetMs()` (OQ-01). Query params are URL-decoded and
sorted by name before signing.

**Security storage** — `TokenStorage` and `DeviceIdProvider` each wrap their own
`EncryptedSharedPreferences` file (AES-256, `MasterKey` AES256_GCM). `DeviceIdProvider`
hashes `ANDROID_ID` once and caches it (OQ-02). Token expiry is surfaced as a hot
`SharedFlow<Unit>` for the future `AuthViewModel`.

**CDN safety** — `CdnValidator` allowlists API/CDN hosts and rejects RFC-1918 /
loopback prefixes; it gates both Coil image loads (in `NetworkModule`) and, later,
stream URLs. `CloudFrontCookieJar` only ever serves the `CloudFront-*` cookies to
the two signed stream hosts and never persists response cookies.

---

## 3. Deviations & decisions

Continuing the numbering from the Phase 0 doc (which ended at D-9). All are
necessary completions or corrections of blueprint snippets that wouldn't compile
as-written; no product behavior changed.

- **D-10 — `SessionState` placed in `:core:domain`; `:core:network` now depends on
  `:core:domain`.** The blueprint's `ClientInfoInterceptor` takes a `SessionState`
  "singleton holding profile flags" but never defines it or its module. It must be
  writable by the profile feature (Phase 4) and readable by the network layer, so
  it lives in pure-Kotlin `core:domain`. This adds one edge (`core:network →
  core:domain`) not drawn in §3.1.

- **D-11 — `SessionState` bound via `@Provides`, not `@Inject` constructor.**
  `core:domain` has no Hilt/Dagger annotation processor, so an `@Inject`-constructor
  class there would never get a generated `_Factory`. It's a plain class bound by
  `NetworkModule.provideSessionState()` in `SingletonComponent` (one shared instance
  app-wide).

- **D-12 — `VERSION_NAME` / `VERSION_CODE` added as `buildConfigField`s in
  `:core:network`.** `ClientInfoInterceptor` reads `BuildConfig.VERSION_NAME/CODE`,
  but a **library** module's `BuildConfig` has no version fields by default. They're
  defined in `core/network/build.gradle.kts` and must be kept in sync with `:app`
  (`1.0.0` / `1`). _Future cleanup:_ inject an app-provided config object instead.

- **D-13 — `ClientInfoInterceptor` gained an `@ApplicationContext Context` param.**
  The blueprint calls `getNetworkType()` but never implements it; determining the
  active transport (wifi/cellular/ethernet) requires `ConnectivityManager`, hence
  the extra constructor arg.

- **D-14 — `x-client-info` built with `buildJsonObject`, not `mapOf` +
  `encodeToString`.** The blueprint's `json.encodeToString(mapOf<String, Any>(...))`
  doesn't compile — kotlinx.serialization has no serializer for `Any`. Replaced with
  an explicit `buildJsonObject { put(...) }` producing the identical JSON.

- **D-15 — `java.util.Base64` instead of `android.util.Base64` in
  `SigningInterceptor`.** `java.util.Base64` is available at our `minSdk 26`, works
  in plain JVM unit tests (no Robolectric), and `getEncoder()` == `NO_WRAP`,
  `getDecoder()` == `DEFAULT` for our inputs. This is what makes T-011 testable.

- **D-16 — `CdnValidator` uses `String.toHttpUrlOrNull()`** rather than the
  OkHttp-3-era `HttpUrl.parse(url)` (removed in OkHttp 4).

- **D-17 — Coil allowlist interceptor rewritten for the Coil 2.x API.** The
  blueprint snippet used a Coil-1.x shape (`override fun intercept`, `chain.request()`,
  `ErrorResult(request, exception)`). Verified against Coil **2.7.0** source and
  corrected to `override suspend fun intercept`, `chain.request` (property),
  `chain.proceed(chain.request)`, and `ErrorResult(drawable?, request, throwable)`.

- **D-18 — Retrofit converter import is `com.jakewharton.retrofit2.converter.
  kotlinx.serialization.asConverterFactory`.** Verified against the 1.0.0 source; the
  extension is on `StringFormat`, so `Json.asConverterFactory(...)` resolves.

- **D-19 — Added JUnit 4 to the version catalog** (`junit = 4.13.2`) and a
  `testImplementation` to `:core:network` for the signing test. (Also added
  `coroutines-test` to the catalog for upcoming phases; not yet used.)

---

## 4. The signing test (T-011)

[`SigningInterceptorTest`](../core/network/src/test/kotlin/com/zinema/app/core/network/SigningInterceptorTest.kt)
asserts three things, the key one being a **known-good** HMAC:

- `KEY_BYTES.size == 30` (the Base64 signing key decodes to 30 bytes).
- For the canonical `stringToSign`
  `GET\n*/*\n\n\n1700000000000\n\n/wefeed-mobile-bff/tab-operating?page=1&tabId=13&version=v1`,
  the HMAC-MD5→Base64 equals **`t/O1mz5q2Vltpw5+7R8Cfg==`**.
- `md5Hex("hello") == 5d41402abc4b2a76b9719d911017c592` (a publicly known MD5).

The expected HMAC was computed with a standalone Java program (`javax.crypto` +
`java.util.Base64`) on a local JDK, **not** by re-running the interceptor's code —
so it's a real regression guard, not a tautology. The crypto helpers (`KEY_BYTES`,
`hmacMd5Base64`, `md5Hex`) are `internal` so the same-module test can call them.

Run it (once you have JDK 17 + SDK):
```bash
./gradlew :core:network:testDebugUnitTest
```

---

## 5. Verify

```bash
./gradlew :core:security:assembleDebug
./gradlew :core:network:assembleDebug
./gradlew :core:network:testDebugUnitTest    # T-011 must pass
./gradlew :app:assembleDebug                 # whole graph still links
```

### Phase 1 "done" checklist
- [ ] Both core modules assemble.
- [ ] `:core:network` unit tests pass (signing HMAC green).
- [ ] App still assembles — Hilt resolves `SessionState`, the interceptors,
      `TokenStorage`, `DeviceIdProvider`, `ApiService`, `ImageLoader`.
- [ ] Manual spot-check: a real request carries `x-tr-signature`, `Authorization`,
      and `x-client-info`, and logs show those values **redacted**.

---

## 6. Known gaps carried forward

| Gap | Resolved in |
|---|---|
| Clock-skew refresh on 407 (calls `/app/config`, stores offset) | Wire in Phase 2 repo / a future interceptor (logic hook already in `TokenStorage`) |
| `signKeyVersion != 2` warning (OQ-03) | Phase 2 config load |
| Actually setting `play_auth` cookies | Phase 7 player (`setPlayAuthCookies`) |
| `SessionState.isKidsProfile` writer | Phase 4 profile selector |
| `BuildConfig.VERSION_*` duplicated in core:network (D-12) | Optional: inject app config object |

---

## 7. Next: Phase 2 — Domain & Data Layer

Tasks **T-020 … T-029**: domain models (§6) + repository interfaces in
`:core:domain`; Room entities/DAOs/`AppDatabase` (§7), `ContentMapper` (§6.1),
the three repository implementations, `DataModule`, and the use cases in
`:core:data`. The mapper depends on `CdnValidator` (done) and the DTOs (done), so
Phase 2 can start immediately.

---

*Generated as part of the Phase 1 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §5/§8 when starting Phase 2.*
