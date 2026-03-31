package com.nerf.launcher.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Low-level persistence helper. All UI configuration should be accessed
 * through ConfigRepository; this class is only for raw SharedPreferences access.
 */
object PreferencesManager {
    const val PREF_NAME = "nerf_launcher_prefs"

    const val KEY_SELECTED_THEME = "selected_theme_name"
    const val KEY_ICON_PACK = "icon_pack_name"
    const val KEY_GRID_SIZE = "grid_size"
    const val KEY_ANIMATION_SPEED = "animation_speed"
    const val KEY_GLOW_INTENSITY = "glow_intensity"
    private const val KEY_PINNED_APPS_LEGACY_SET = "pinned_apps"
    const val KEY_PINNED_APPS_ORDERED = "pinned_apps_ordered"
    const val KEY_TASKBAR_ICON_SIZE = "taskbar_icon_size"
    const val KEY_TASKBAR_BACKGROUND_STYLE = "taskbar_background_style"
    const val KEY_TASKBAR_HEIGHT = "taskbar_height"
    const val KEY_TASKBAR_TRANSPARENCY = "taskbar_transparency"
    const val KEY_TASKBAR_ENABLED = "taskbar_enabled"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSelectedTheme(context: Context, themeName: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_THEME, themeName).apply()
    }

    fun getSelectedTheme(context: Context): String? =
        getPrefs(context).getString(KEY_SELECTED_THEME, null)

    fun saveIconPack(context: Context, packName: String) {
        getPrefs(context).edit().putString(KEY_ICON_PACK, packName).apply()
    }

    fun getIconPack(context: Context): String? =
        getPrefs(context).getString(KEY_ICON_PACK, null)

    fun saveGridSize(context: Context, size: Int) {
        getPrefs(context).edit().putInt(KEY_GRID_SIZE, size).apply()
    }

    fun getGridSize(context: Context): Int =
        getPrefs(context).getInt(KEY_GRID_SIZE, 4)

    fun saveAnimationSpeed(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ANIMATION_SPEED, enabled).apply()
    }

    fun isAnimationSpeedEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ANIMATION_SPEED, false)

    fun saveGlowIntensity(context: Context, intensity: Float) {
        getPrefs(context).edit().putFloat(KEY_GLOW_INTENSITY, intensity).apply()
    }

    fun getGlowIntensity(context: Context): Float =
        getPrefs(context).getFloat(KEY_GLOW_INTENSITY, 0.0f)

    fun savePinnedApps(context: Context, apps: List<String>) {
        val sanitized = apps
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        getPrefs(context).edit()
            .putString(KEY_PINNED_APPS_ORDERED, sanitized.joinToString(","))
            .remove(KEY_PINNED_APPS_LEGACY_SET)
            .apply()
    }

    fun getPinnedApps(context: Context): List<String> {
        val prefs = getPrefs(context)
        val ordered = prefs.getString(KEY_PINNED_APPS_ORDERED, null)
        if (!ordered.isNullOrBlank()) {
            return ordered
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }
        val legacyPinned = prefs.getStringSet(KEY_PINNED_APPS_LEGACY_SET, emptySet())
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()

        if (legacyPinned.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_PINNED_APPS_ORDERED, legacyPinned.joinToString(","))
                .remove(KEY_PINNED_APPS_LEGACY_SET)
                .apply()
        }

        return legacyPinned
    }

    fun saveTaskbarIconSize(context: Context, size: Int) {
        getPrefs(context).edit().putInt(KEY_TASKBAR_ICON_SIZE, size).apply()
    }

    fun getTaskbarIconSize(context: Context): Int =
        getPrefs(context).getInt(KEY_TASKBAR_ICON_SIZE, 48)

    fun saveTaskbarBackgroundStyle(context: Context, style: TaskbarBackgroundStyle) {
        getPrefs(context).edit().putInt(KEY_TASKBAR_BACKGROUND_STYLE, style.persistedValue).apply()
    }

    fun getTaskbarBackgroundStyle(context: Context): TaskbarBackgroundStyle =
        getPrefs(context).run {
            val rawValue = getInt(
                KEY_TASKBAR_BACKGROUND_STYLE,
                TaskbarBackgroundStyle.default.persistedValue
            )
            val resolvedStyle = TaskbarBackgroundStyle.fromPersistedValue(rawValue)
            if (rawValue != resolvedStyle.persistedValue) {
                edit().putInt(KEY_TASKBAR_BACKGROUND_STYLE, resolvedStyle.persistedValue).apply()
            }
            resolvedStyle
        }

    fun saveTaskbarHeight(context: Context, heightDp: Int) {
        getPrefs(context).edit().putInt(KEY_TASKBAR_HEIGHT, heightDp).apply()
    }

    fun getTaskbarHeight(context: Context): Int =
        getPrefs(context).getInt(KEY_TASKBAR_HEIGHT, 56)

    fun saveTaskbarTransparency(context: Context, transparency: Float) {
        getPrefs(context).edit().putFloat(KEY_TASKBAR_TRANSPARENCY, transparency).apply()
    }

    fun getTaskbarTransparency(context: Context): Float =
        getPrefs(context).getFloat(KEY_TASKBAR_TRANSPARENCY, 1.0f)

    fun saveTaskbarEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TASKBAR_ENABLED, enabled).apply()
    }

    fun isTaskbarEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_TASKBAR_ENABLED, true)

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
