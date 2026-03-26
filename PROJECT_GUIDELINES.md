# NerfLauncher – Implementation Overview

This document describes the major components of the NerfLauncher, their responsibilities, and how they interact to deliver a fully themable, icon‑customizable Android launcher with HUD, taskbar, status bar, and live settings.

## 1. High‑Level Data Flow
[PackageManager] --> AppUtils --> List
|                                           |
v                                           v
[LiveData in LauncherViewModel] <--- (postValue) --- [ViewModelScope.launch]
|
v
[MainActivity observes LiveData] --> submits to AppAdapter
|
v
[AppAdapter] uses IconProvider to get Drawable for each AppInfo
|
v
[RecyclerView] displays ItemAppBinding (icon + label)
User interactions (theme change, icon pack change, grid size) modify `PreferencesManager`.  
`UIUpdateManager` exposes `LiveData<NerfTheme>`, `LiveData<String>`, `LiveData<Int>` for theme, icon pack, and grid span.  
Observers in `MainActivity` react instantly:
- Theme → `ThemeManager.applyTheme()` (updates HUD, window background, status bar via `StatusBarManager`).
- Icon pack → `IconProvider.evictCache()` + `adapter.notifyDataSetChanged()`.
- Grid span → `GridLayoutManager.spanCount` update + persist scale factor.

## 2. Module Breakdown

### 2.1 `model/`
- **`AppInfo.kt`** – immutable data class holding app label, package name, class name, and icon `Drawable`.  
  Used throughout the adapter and utilities.

### 2.2 `util/` (core helpers)
| Class | Responsibility |
|-------|----------------|
| `AppUtils.kt` | Queries `PackageManager` for launchable apps, launches an app via intent, opens system home settings. |
| `IconPackManager.kt` | Holds the list of available icon packs (`system`, `nerf`, `minimal`) and reads/writes the current pack via `PreferencesManager`. |
| `IconCache.kt` | Simple `LruCache<String, Drawable>` keyed by `"<pack>:<package>"`. |
| `IconProvider.kt` | Given a package name, returns the appropriate `Drawable` (custom pack asset → system icon fallback). Handles caching and cache eviction. |
| `NerfTheme.kt` | Data class representing a theme (name, primary, secondary, accent, glow intensity, background style). |
| `ThemeRepository.kt` | Object providing five predefined themes (`CLASSIC_NERF`, `STEALTH_OPS`, `ELITE_BLUE`, `ZOMBIE_STRIKE`, `HYPER_NEON`) and a lookup by name. |
| `PreferencesManager.kt` | Thin wrapper around `SharedPreferences` (`nerf_launcher_prefs`). Stores selected theme name, icon pack name, UI scale (used for grid size), glow intensity (0‑1), and other UI toggles. |
| `ThemeManager.kt` | Applies a `NerfTheme` to an `Activity`: updates window background, HUD views (battery meter progress tint, time text color, widget button color, glow overlay), and notifies `UIUpdateManager`. |
| `UIUpdateManager.kt` | Singleton with three `MutableLiveData` fields (`theme`, `iconPack`, `gridSpan`). Used as the central event bus for instant UI updates. |
| `StatusBarManager.kt` | Sets status bar color and icon lightness (API ≥ M) using `WindowInsetsControllerCompat`. Provides a reset method. |
| `TaskbarController.kt` | Manages a list of pinned app package names persisted in `SharedPreferences`. Offers add/remove/get/clear operations. |
| `SettingType.kt` & `SettingItem.kt` | Simple enum and data class used by the Settings screen to describe each row’s type and payload. |

### 2.3 `viewmodel/`
- **`LauncherViewModel.kt`** – loads the list of `AppInfo` via `AppUtils.loadInstalledApps()` inside a `viewModelScope.launch` coroutine and exposes it as `LiveData<List<AppInfo>>`.  
  The `HomeViewModel` (legacy) is retained for backward compatibility but not used in the current flow.

### 2.4 `ui/` (Screens and custom views)
| Component | Purpose |
|----------|---------|
| `MainActivity.kt` | Main launcher screen. Sets up RecyclerView, observes `LauncherViewModel`, initializes HUD, taskbar, status bar, and registers `UIUpdateManager` observers. Handles theme change requests from `SettingsActivity`. |
| `HudController.kt` | Manages the top HUD: battery meter (ProgressBar), digital time (TextView), widget area button, and touch animations. Receives updates from `ThemeManager` and `StatusBarManager`. |
| `TaskbarView.kt` | Custom `LinearLayout` that holds a variable number of `ImageView` slots (default 4). Exposes `setIconProvider()` and `updateIcons(List<String>)`. |
| `SettingsActivity.kt` | Shows a `RecyclerView` of `SettingItem` rows. Each row is rendered by `SettingsAdapter` and contains the appropriate input widget (Spinner, SeekBar, Switch, etc.). When a setting changes, it updates `PreferencesManager` and notifies `UIUpdateManager` (theme/icon/grid) or directly calls `TaskbarController`/`ThemeManager` where immediate effect is needed. |
| `SettingsAdapter.kt` | Binds `SettingItem` to `item_setting.xml`. Handles widget visibility and listener wiring for each setting type. |

### 2.5 `adapter/`
- **`AppAdapter.kt`** – extends `ListAdapter<AppInfo, AppAdapter.AppViewHolder>` with a `DiffUtil.ItemCallback` based on package+class.  
  In `onBindViewHolder`, it fetches the appropriate `Drawable` from the injected `IconProvider` and sets click listeners to launch the app via `AppUtils.launchApp`.

### 2.6 `res/` (Resources)
- **`values/`** – `colors.xml` (Nerf palette + derived shades), `styles.xml` (Theme.NerfLauncher based on MaterialComponents.DayNight, custom button style, text appearances), `strings.xml` (all user‑visible text, including settings), `dimens.xml` (spacing constants, widget heights), `themes.xml` (aliases for material theme).  
- **`layout/`** – `activity_main.xml` (RecyclerView + HUD include + TaskbarView), `item_app.xml` (icon + name), `hud_layout.xml` (top HUD), `activity_settings.xml` (CoordinatorLayout with Toolbar + RecyclerView), `item_setting.xml` (flexible row with conditional visibility).  
- **`drawable/`** – shape‑based backgrounds for buttons, panels, item selectors, and gradient background for animated HUD.  
- **`anim/`** – XML animations for button press recoil, icon hover glow, widget expand/collapse, HUD tap recoil, and pulsed gradient background.  
- **`mipmap-*/`** – launcher icons (adaptive and legacy) for all densities.  
- **`assets/icon_packs/`** – folders containing PNG icons for each pack; file name must exactly match the app’s package name (e.g., `com.facebook.katana.png`). The `system` folder is empty; the provider falls back to `PackageManager`.

## 3. Interaction Summary
- **Startup:** `MainActivity` reads saved preferences, seeds `UIUpdateManager`, applies theme, sets up `IconProvider`, observes `LiveData` from `LauncherViewModel`, and displays the app grid.
- **Theme Change:** `SettingsActivity` → `PreferencesManager.saveSelectedTheme` → `MainActivity.changeTheme()` → `PreferencesManager` → `UIUpdateManager.setTheme` (via `ThemeManager.applyTheme`) → observers update HUD, window background, status bar.
- **Icon Pack Change:** `SettingsActivity` → `PreferencesManager.saveIconPack` → `UIUpdateManager.setIconPack` → observer → `IconProvider.evictCache()` → `adapter.notifyDataSetChanged()` → grid refreshes with new icons.
- **Glow Intensity:** SeekBar updates `PreferencesManager.saveGlowEnabled`, `UIUpdateManager` not observer needed because `ThemeManager.applyTheme()` reads current glow intensity on each theme change; also `ThemeManager` is called from the observer.
- **Animation Speed / Grid Size:** Stored as a UI scale factor (`PreferencesManager.saveUiScale`). When changed, `UIUpdateManager.setGridSpan` triggers observer → `GridLayoutManager.spanCount` update → grid redraws instantly. The same scale persists across rotations.
- **Taskbar:** `TaskbarController` reads/writes pinned apps. On start and after the app list loads, `MainActivity.updateTaskbarIcons()` asks the controller for the current pinned list (or falls back to first four apps) and tells `TaskbarView` to set those icons via `IconProvider`. Long‑press on a slot shows a toast (future extension to edit pins).
- **Status Bar:** Whenever the theme (including glow) changes, `MainActivity.applyStatusBarTheme()` recomputes whether the primary color is light/dark and calls `StatusBarManager.applyStatusBarTheme`. On destruction, the status bar is reset to avoid leaking the custom theme to other apps.

## 4. Performance Considerations
- **Icon caching:** `IconProvider` uses an `LruCache` (size 50) keyed by pack+package; ensures that scrolling does not trigger repeated asset reads or `PackageManager` calls.
- **RecyclerView optimizations:** `setHasFixedSize(true)`, `setItemViewCacheSize(20)` (enough for off‑screen items in a 4‑column grid), `setDrawingCacheEnabled(false)`.
- **LiveData observers:** All observers are lifecycle‑bound to the Activity; they auto‑remove on `onDestroy`.
- **HudController:** Uses a `Handler` to update the time once per minute, aligned to minute boundary to reduce wake‑ups.
- **Taskbar:** Only updates when the pinned list changes or on initial load; otherwise it’s a static view.

## 5. Extending the Launcher
- **New Theme:** add a new `NerfTheme` instance to `ThemeRepository`, give it a unique name, and optionally add it to the `all` list. It will appear automatically in the Settings theme spinner.
- **New Icon Pack:** create a folder under `src/main/assets/icon_packs/<newpack>/`, place `<package>.png` files for the apps you wish to theme, then add the pack name to `IconPackManager.getAvailablePacks()`. The Settings spinner will list it.
- **Additional Setting:** extend `SettingType`, add a row to `SettingsAdapter` (copy an existing row and change visibility/logic), persist the value via `PreferencesManager`, and expose it via `UIUpdateManager` if it needs to trigger UI updates.
- **New Custom View:** follow the pattern of `TaskbarView`: inherit from a suitable ViewGroup, expose setters for data, and keep all theme‑dependent styling in the XML or via `ThemeManager` when needed.

By keeping concerns separated, using LiveData for instant propagation, and caching expensive operations, NerfLauncher provides a fluid, highly customizable experience while staying within Android’s performance expectations.