# AI Agent Guidelines for NerfLauncher Project

This document provides instructions for AI assistants (e.g., Perplexity, GitHub Copilot, Claude) when helping with the NerfLauncher Android launcher project.

## 1. Project Overview
- **Language:** Kotlin (minSdk 26, targetSdk 34)
- **Architecture:** MVVM with ViewBinding (no Jetpack Compose)
- **Core Packages:** `com.nerf.launcher`
  - `model` – data classes (AppInfo)
  - `ui` – Activities, custom views, adapters
  - `adapter` – RecyclerView adapters
  - `util` – helpers, managers, providers, caches
  - `viewmodel` – ViewModels exposing LiveData
  - `res` – all resources (layouts, drawables, values, anim, mipmap, assets)

## 2. Where to Find Key Files
| Feature | File Path |
|---------|-----------|
| Entry point (Launcher) | `app/src/main/AndroidManifest.xml` |
| Main launcher screen | `app/src/main/java/com/nerf/launcher/ui/MainActivity.kt` |
| HUD overlay | `app/src/main/java/com/nerf/launcher/ui/HudController.kt` + `app/src/main/res/layout/hud_layout.xml` |
| Taskbar | `app/src/main/java/com/nerf/launcher/ui/TaskbarView.kt` + `app/src/main/java/com/nerf/launcher/ui/TaskbarController.kt` |
| Status bar | `app/src/main/java/com/nerf/launcher/util/StatusBarManager.kt` |
| Settings screen | `app/src/main/java/com/nerf/launcher/ui/SettingsActivity.kt` + `app/src/main/java/com/nerf/launcher/ui/SettingsAdapter.kt` |
| Icon system | `app/src/main/java/com/nerf/launcher/util/IconProvider.kt`, `IconCache.kt`, `IconPackManager.kt` |
| Theme system | `app/src/main/java/com/nerf/launcher/util/ThemeManager.kt`, `ThemeRepository.kt`, `PreferencesManager.kt`, `UIUpdateManager.kt` |
| App data loading | `app/src/main/java/com/nerf/launcher/util/AppUtils.kt` |
| ViewModels | `app/src/main/java/com/nerf/launcher/viewmodel/LauncherViewModel.kt` (main) and `HomeViewModel.kt` (legacy) |
| RecyclerView adapter | `app/src/main/java/com/nerf/launcher/adapter/AppAdapter.kt` |
| Layouts | `app/src/main/res/layout/activity_main.xml`, `item_app.xml`, `activity_settings.xml`, `item_setting.xml` |
| Resources | `app/src/main/res/values/colors.xml`, `styles.xml`, `strings.xml`, `dimens.xml`, `themes.xml` |
| Drawables & Animations | `app/src/main/res/drawable/` and `app/src/main/res/anim/` |
| Asset icon packs | `app/src/main/assets/icon_packs/{system,nerf,minimal}/<package>.png` |

## 3. How to Modify Safely
- **Never edit generated bindings** – they are created at compile time from layout files.
- **When adding a new UI screen:** create a new Activity in `ui/`, a corresponding layout under `res/layout/`, and register it in `AndroidManifest.xml` if it needs to be launchable.
- **When adding a new utility:** place it in the appropriate `util` sub‑package and expose only what is needed via public functions/objects.
- **When changing theme or icon pack:** use `UIUpdateManager` LiveData observers; do **not** recreate the whole activity unless absolutely necessary.
- **Always keep null‑safety:** use `?.`, `!!` only when you are certain the value cannot be null, and prefer `let/run` chains.
- **Do not block the main thread:** all long‑running work (PackageManager queries, asset reads) must be off‑loaded to coroutines (`viewModelScope.launch`) or workers.
- **Keep resources theme‑based:** avoid hard‑coding colors in XML; reference theme attributes (`?attr/colorPrimary`) or values from `colors.xml` that are themselves defined via the theme.

## 4. Testing Guidance
- Unit test pure Kotlin logic in `util/` (e.g., `PreferencesManager`, `IconProvider` with mocked `Context`).
- UI tests (Espresso) should focus on navigation and state changes; use `IdlingResource` for coroutine‑based loading.
- Screenshot tests are discouraged because of launcher‑specific system UI; rely on manual verification on emulators/physical devices.

## 5. Getting Help from the Agent
When asking for code snippets:
1. Specify the exact file and function you want to modify.
2. Mention any relevant LiveData or state objects.
3. If you need a new resource, describe its purpose and where it should be placed (e.g., “add a new dimen for taskbar height in `values/dimens.xml`”).
4. Ask for full‑file replacements only when the existing file is entirely wrong; otherwise request a diff‑style snippet.

Follow these guidelines and the agent will be able to provide accurate, compile‑ready code that integrates cleanly with the existing launcher.