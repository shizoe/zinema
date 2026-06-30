# Phase 4 — Auth Feature (Implementation Guide)

> **Status:** ✅ Complete and **build-verified** (2026-06-30). `:feature:auth:assembleDebug`
> and `:app:assembleDebug` pass. This is the first real `@HiltViewModel`, so the
> app build now validates the use-case/repository graph for actual ViewModel
> injection (Hilt confirms `AuthRepository` is providable to `AuthViewModel`).
>
> **Blueprint source:** `ANDROID_BLUEPRINT.md` §11 → "Phase 4 — Auth Feature"
> (tasks **T-037, T-038, T-038b, T-039**).

---

## 1. Task completion map

| Task | Description | Status | File(s) |
|---|---|---|---|
| **T-037** | `AuthViewModel` (guest login, credential login, expiry) | ✅ | [AuthViewModel.kt](../feature/auth/src/main/kotlin/com/zinema/app/feature/auth/AuthViewModel.kt) |
| **T-038** | `LoginScreen` | ✅ | [LoginScreen.kt](../feature/auth/src/main/kotlin/com/zinema/app/feature/auth/LoginScreen.kt) |
| **T-038b** | `strings.xml` (app_name/tagline) | ✅ | app_name in `:app` (Phase 0); auth strings in [feature/auth strings.xml](../feature/auth/src/main/res/values/strings.xml) |
| **T-039** | `ProfileSelectorScreen` | ✅ | [ProfileSelectorScreen.kt](../feature/auth/src/main/kotlin/com/zinema/app/feature/auth/ProfileSelectorScreen.kt) |

Supporting (new): `AuthRepository` (core:domain) + `AuthRepositoryImpl` (core:data,
bound in `DataModule`).

---

## 2. Design notes

- **Layered auth.** `AuthViewModel` injects an `AuthRepository` (core:domain),
  whose impl (core:data) wraps the encrypted `TokenStorage`. This keeps
  `feature:auth → core:domain` (it never depends on `core:security`/`core:data`).
- **Guest login.** Reads `BuildConfig.GUEST_JWT` (a `buildConfigField` now on
  `:feature:auth`, sourced from the `ZINEMA_GUEST_JWT` Gradle property) and persists
  it via the repository. Empty token → a friendly error.
- **UDF state.** `AuthUiState` = Idle | Loading | Success(token) | Error(message);
  `LoginScreen` collects it with `collectAsStateWithLifecycle`, navigates on Success,
  and shows errors via the Scaffold `SnackbarHost`.
- **Session expiry.** `AuthViewModel.sessionExpiredEvents` re-exposes
  `TokenStorage.tokenExpiredEvents` (via the repo) for the nav host to observe and
  route back to login.
- **Profiles.** `ProfileSelectorScreen` is a stateless composable: avatars (≤5),
  an Add Profile tile, Manage Profiles, Kids badges, and a PIN `AlertDialog` for
  locked profiles. The profile source + `SessionState.isKidsProfile` update are
  supplied by the caller/nav layer.

---

## 3. Deviations & decisions (continuing from D-31)

- **D-32 — Introduced `AuthRepository` (domain) + `AuthRepositoryImpl` (data).** The
  blueprint's `AuthViewModel` touches `TokenStorage` directly, which would force
  `feature:auth → core:security` against the module graph. The repository keeps
  layering clean; `AuthViewModel` injects it instead of `TokenStorage`.
- **D-33 — `GUEST_JWT` `buildConfigField` added to `:feature:auth`** (+ `buildConfig
  = true`) so `AuthViewModel` reads its own module's `BuildConfig`. Keep in sync with
  the `:app` field (both read `ZINEMA_GUEST_JWT`).
- **D-34 — `loginWithCredentials` throws `UnsupportedOperationException`.** No OneID
  credential endpoint is defined in the blueprint (`ApiService`, §8.1). Guest login
  is the working path; credential login is wired when the endpoint is known. **GAP.**
- **D-35 — Wordmark is styled `Text`**, not a vector drawable (T-038 says "vector
  drawable"). Rendering "ZINEMA" as bold red letter-spaced text is a faithful
  wordmark without fragile hand-authored path art.
- **D-36 — `ProfileSelectorScreen` is stateless (no `ProfileViewModel`).** None is in
  the task list and no profile persistence is specified; the screen takes
  `profiles` + callbacks. Profile storage and the Kids-mode `SessionState` write are
  deferred to the nav/integration layer.

---

## 4. Known gaps carried forward

| Gap | Impact | Resolve in |
|---|---|---|
| Credential login endpoint (OneID) | `loginWithCredentials` not functional; guest login works | when API path known |
| Profile persistence + source | `ProfileSelectorScreen` needs a real `List<UserProfile>` | a profile store (DataStore/Room) + nav wiring |
| `SessionState.isKidsProfile` write on kids-profile select | x-content-mode stays 0 until wired | nav/integration layer |
| Navigation graph hosting these screens | screens take callbacks; no NavHost yet | `navigation/` (later) |
| Coil `ImageLoader` wiring (from Phase 3) | avatars/posters use Coil default loader | app wiring (Phase 5/10) |

---

## 5. Verify

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
./gradlew :feature:auth:assembleDebug
./gradlew :app:assembleDebug     # validates AuthViewModel @HiltViewModel graph
```

### Phase 4 "done" checklist
- [x] `:feature:auth` compiles (ViewModel + 2 screens + strings). _(verified 2026-06-30)_
- [x] `:app:assembleDebug` passes — Hilt provides `AuthRepository` to `AuthViewModel`.
- [ ] Visual + flow check on device (deferred until nav hosts the screens).

---

## 6. Next: Phase 5 — Home & Browse Feature

Tasks **T-040 … T-043** in `:feature:home`: `HomeViewModel` + `TabContentViewModel`
(the §9.3 `StateFlow<UiState>` pattern over `GetTabContentUseCase`), the
`CONTENT_TABS` list (drop tabId=27 per OQ-06), `HomeScreen` (mobile: bottom nav +
`HeroBanner` + `ContentRail`s + shimmer/empty states), and `TvHomeScreen` (side nav
+ rails). The Phase 3 components and Phase 2 use cases are ready to wire. Good time
to also wire the Coil `ImageLoader` (make `ZinemaApplication` an `ImageLoaderFactory`).

---

*Generated as part of the Phase 4 implementation pass. Pair with
`ANDROID_BLUEPRINT.md` §9 when starting Phase 5.*
