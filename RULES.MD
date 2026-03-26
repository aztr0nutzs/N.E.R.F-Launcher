# NerfLauncher – Coding & Style Rules

## 1. File Organization
- Every Kotlin file must belong to a package matching its directory under `java/com/nerf/launcher/`.
- Group related files in sub‑packages (`model`, `ui`, `adapter`, `util`, `viewmodel`).
- No loose files in the root of `java/`.

## 2. Naming Conventions
- **Classes / Objects / Interfaces:** `PascalCase` (e.g., `IconProvider`, `ThemeManager`).
- **Functions / Properties / Variables:** `camelCase` (e.g., `getIcon`, `isGlowEnabled`).
- **Constants:** `UPPER_SNAKE` with `val` (e.g., `DEFAULT_PACK`, `PINNED_APPS_KEY`).
- **Enum entries:** `PascalCase` (e.g., `THEME`, `ICON_PACK`).
- **XML resources:** `snake_case` (e.g., `nerf_primary`, `text_appearance_body1`, `item_app`).

## 3. Import Order
1. `androidx.*`
2. `android.*`
3. `java.*`
4. `kotlin.*`
5. Third‑party (e.g., `com.squareup.*`)
6. Project‑local (`com.nerf.launcher.*`)

Within each group, sort alphabetically.

## 4. Kotlin Specific Rules
- **Data classes:** use for simple DTOs (e.g., `AppInfo`, `NerfTheme`, `SettingItem`).
- **Sealed classes / objects:** prefer for UI state or result types.
- **When expressions:** cover all enum values or use an `else` branch when appropriate.
- **Lateinit:** only for properties that are guaranteed to be initialized before first use (e.g., ViewBinding, adapters).
- **Singleton objects:** declare as `object` (not `private const val`); use for stateless utilities or managers.
- **Extension functions:** place in a file named after the extended type when possible (e.g., `ContextExtensions.kt` for extensions on `Context`).

## 5. XML Rules
- **Layout file names:** `snake_case` describing the screen or component (e.g., `activity_main.xml`, `item_app.xml`).
- **ID names:** `snake_case` prefixed with view type when helpful (e.g., `btn_home_settings`, `rv_recyclerView`, `tv_time_display`).
- **Avoid hardcoding dimensions:** use `@dimen/` references; define all dimensions in `res/values/dimens.xml`.
- **Avoid hardcoding colors:** use `@color/` or theme attributes (`?attr/colorPrimary`).
- **String formatting:** use XLiff comments for placeholders when needed (e.g., `"%1$s apps"`).
- **Drawable selectors:** place in `res/drawable/` and reference via `@drawable/`.

## 6. Commenting
- **KDoc** for public classes, functions, and properties (describe purpose, parameters, return value, and any side‑effects).
- **TODO** comments are allowed only with an associated issue tracker link (e.g., `// TODO: (NERF-123) implement icon pack download`).
- Avoid commented‑out code; delete it instead.
- Use `//` for short inline comments; use `/** … */` for block comments only when KDoc is not appropriate.

## 7. Logging
- Use `android.util.Log` only in debug builds.
- Define a companion object constant: `private const val TAG = "ClassName"`.
- Prefer `Log.d(TAG, "msg")` for debug, `Log.w` for warnings, `Log.e` for errors.
- Strip logs in release builds via `proguard-rules.pro` (`-assumenosideeffects class android.util.Log { *; }`).

## 8. Testing
- Unit test file name: `<ClassNameTest.kt>` placed in `src/test/java/` mirroring the package.
- UI test file name: `<ClassNameTest.kt>` placed in `src/androidTest/java/`.
- Use `JUnit5` and `Truth` or `AssertJ` for assertions.
- Mock Android dependencies with `MockK` or `Mockito`.

## 9. Build Configuration
- `compileSdk` and `targetSdk` must be kept in sync with the latest stable SDK.
- `minSdk` must never be increased without a corresponding deprecation plan.
- `versionCode` increments by 1 for each release; `versionName` follows semantic versioning (e.g., `1.2.0`).
- `buildTypes.debug` must have `minifyEnabled false`; `release` must have `minifyEnabled true` with the provided `proguard-rules.pro`.

## 10. Review Checklist (for PRs)
- [ ] No hardcoded colors/dimens/strings in XML or Kotlin.
- [ ] All new layouts have ViewBinding enabled.
- [ ] RecyclerView uses `setHasFixedSize(true)` and appropriate cache size.
- [ ] IconProvider cache cleared on icon pack change.
- [ ] Theme changes flow through `UIUpdateManager` → observers.
- [ ] No blocking calls on the main thread.
- [ ] All public API functions are KDoc‑documented.
- [ ] XML files pass Lint checks (no missing translations, no unused resources).
- [ ] ProGuard rules keep necessary classes.
- [ ] Added unit tests for any new logic.
- [ ] Added UI tests for any new screen or interaction.