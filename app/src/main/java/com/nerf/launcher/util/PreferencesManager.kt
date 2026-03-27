package com.nerf.launcher.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Low‑level persistence helper. All UI configuration should be accessed
 * through ConfigRepository; this class is only for raw SharedPreferences access.
 */
object PreferencesManager {
    const val PREF_NAME = "nerf_launcher_prefs"

    // Existing keys
    const val KEY_SELECTED_THEME = "selected_theme_name"
    const val KEY_ICON_PACK = "icon_pack_name"

    // New keys for unified config
    const val KEY_GRID_SIZE = "grid_size"
    const val KEY_ANIMATION_SPEED = "animation_speed"
    const val KEY_GLOW_INTENSITY = "glow_intensity"
    const val KEY_PINNED_APPS = "pinned_apps"
    const val KEY_TASKBAR_ICON_SIZE = "taskbar_icon_size"
    const val KEY_TASKBAR_BACKGROUND_STYLE = "taskbar_background_style"
    const val KEY_TASKBAR_HEIGHT = "taskbar_height"
    const val KEY_TASKBAR_TRANSPARENCY = "taskbar_transparency"
    const val KEY_TASKBAR_ENABLED = "taskbar_enabled"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Theme
    fun saveSelectedTheme(context: Context, themeName: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_THEME, themeName).apply()
    }
    fun getSelectedTheme(context: Context): String? =
        getPrefs(context).getString(KEY_SELECTED_THEME, null)

    // Icon pack
    fun saveIconPack(context: Context, packName: String) {
        getPrefs(context).edit().putString(KEY_ICON_PACK, packName).apply()
    }
    fun getIconPack(context: Context): String? =
        getPrefs(context).getString(KEY_ICON_PACK, null)

    // Grid size
    fun saveGridSize(context: Context, size: Int) {
        getPrefs(context).edit().putInt(KEY_GRID_SIZE, size).apply()
    }
    fun getGridSize(context: Context): Int =
        getPrefs(context).getInt(KEY_GRID_SIZE, 4)

    // Animation speed
    fun saveAnimationSpeed(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ANIMATION_SPEED, enabled).apply()
    }
    fun isAnimationSpeedEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ANIMATION_SPEED, false)

    // Glow intensity
    fun saveGlowIntensity(context: Context, intensity: Float) {
        getPrefs(context).edit().putFloat(KEY_GLOW_INTENSITY, intensity).apply()
    }
    fun getGlowIntensity(context: Context): Float =
        getPrefs(context).getFloat(KEY_GLOW_INTENSITY, 0.0f)

    // Pinned apps (taskbar)
    fun savePinnedApps(context: Context, apps: List<String>) {
        getPrefs(context).edit()
            .putStringSet(KEY_PINNED_APPS, apps.toMutableSet()).apply()
    }
    fun getPinnedApps(context: Context): List<String> =
        getPrefs(context).getStringSet(KEY_PINNED_APPS, mutableSetOf())?.toList() ?: emptyList()

    // Taskbar icon size
    fun saveTaskbarIconSize(context: Context, size: Int) {
        getPrefs(context).edit().putInt(KEY_TASKBAR_ICON_SIZE, size).apply()
    }
    fun getTaskbarIconSize(context: Context): Int =
        getPrefs(context).getInt(KEY_TASKBAR_ICON_SIZE, 48)

    // Taskbar background style
    fun saveTaskbarBackgroundStyle(context: Context, styleResId: Int) {
        getPrefs(context).edit()
            .putInt(KEY_TASKBAR_BACKGROUND_STYLE, styleResId).apply()
    }
    fun getTaskbarBackgroundStyle(context: Context): Int =
        getPrefs(context).getInt(KEY_TASKBAR_BACKGROUND_STYLE,
                android.R.color.background_dark)

    // Convenience: clear all (mainly for testing)
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}