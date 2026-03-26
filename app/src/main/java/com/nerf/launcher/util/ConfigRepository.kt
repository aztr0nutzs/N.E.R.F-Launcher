package com.nerf.launcher.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Single source of truth for launcher configuration.
 * Loads from and saves to SharedPreferences via PreferencesManager.
 * Exposes a LiveData<AppConfig> that UI layers observe.
 */
class ConfigRepository(private val context: Context) {

    companion object {
        private var instance: ConfigRepository? = null
        fun init(app: Application) {
            if (instance == null) {
                instance = ConfigRepository(app)
            }
        }
        fun get(): ConfigRepository =
            instance ?: throw IllegalStateException("ConfigRepository not initialized. Call init() in Application.")
    }

    private val _config = MutableLiveData<AppConfig>(loadConfig())
    val config: LiveData<AppConfig> = _config

    private fun loadConfig(): AppConfig {
        val prefs = context.getSharedPreferences(PreferencesManager.PREF_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(PreferencesManager.KEY_SELECTED_THEME,
                ThemeRepository.CLASSIC_NERF.name)
        val iconPack = prefs.getString(PreferencesManager.KEY_ICON_PACK,
                IconPackManager.DEFAULT_PACK)
        val gridSize = prefs.getInt(PreferencesManager.KEY_GRID_SIZE, 4)
        val animationSpeedEnabled = prefs.getBoolean(PreferencesManager.KEY_ANIMATION_SPEED, false)
        val glowIntensity = prefs.getFloat(PreferencesManager.KEY_GLOW_INTENSITY, 0.0f)
        val taskbarSettings = TaskbarSettings(
            height = prefs.getInt(PreferencesManager.KEY_TASKBAR_HEIGHT, 56),
            iconSize = prefs.getInt(PreferencesManager.KEY_TASKBAR_ICON_SIZE, 48),
            backgroundStyle = prefs.getInt(PreferencesManager.KEY_TASKBAR_BACKGROUND_STYLE,
                    android.R.color.background_dark),
            transparency = prefs.getFloat(PreferencesManager.KEY_TASKBAR_TRANSPARENCY, 1.0f),
            enabled = prefs.getBoolean(PreferencesManager.KEY_TASKBAR_ENABLED, true),
            pinnedApps = prefs.getStringSet(PreferencesManager.KEY_PINNED_APPS, emptySet())?.toList() ?: emptyList()
        )
        return AppConfig(themeName, iconPack, gridSize, animationSpeedEnabled,
                glowIntensity, taskbarSettings)
    }

    fun saveConfig(config: AppConfig) {
        val prefs = context.getSharedPreferences(PreferencesManager.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PreferencesManager.KEY_SELECTED_THEME, config.themeName)
            .putString(PreferencesManager.KEY_ICON_PACK, config.iconPack)
            .putInt(PreferencesManager.KEY_GRID_SIZE, config.gridSize)
            .putBoolean(PreferencesManager.KEY_ANIMATION_SPEED, config.animationSpeedEnabled)
            .putFloat(PreferencesManager.KEY_GLOW_INTENSITY, config.glowIntensity)
            .putInt(PreferencesManager.KEY_TASKBAR_HEIGHT, config.taskbarSettings.height)
            .putInt(PreferencesManager.KEY_TASKBAR_ICON_SIZE, config.taskbarSettings.iconSize)
            .putInt(PreferencesManager.KEY_TASKBAR_BACKGROUND_STYLE,
                    config.taskbarSettings.backgroundStyle)
            .putFloat(PreferencesManager.KEY_TASKBAR_TRANSPARENCY,
                    config.taskbarSettings.transparency)
            .putBoolean(PreferencesManager.KEY_TASKBAR_ENABLED,
                    config.taskbarSettings.enabled)
            .putStringSet(PreferencesManager.KEY_PINNED_APPS,
                    config.taskbarSettings.pinnedApps.toSet())
            .apply()
        _config.value = config
    }

    // Convenience update methods
    fun updateTheme(themeName: String) {
        val current = _config.value ?: return
        saveConfig(current.copy(themeName = themeName))
    }

    fun updateIconPack(pack: String) {
        val current = _config.value ?: return
        saveConfig(current.copy(iconPack = pack))
    }

    fun updateGridSize(size: Int) {
        val current = _config.value ?: return
        saveConfig(current.copy(gridSize = size.coerceIn(2, 6)))
    }

    fun updateAnimationSpeed(enabled: Boolean) {
        val current = _config.value ?: return
        saveConfig(current.copy(animationSpeedEnabled = enabled))
    }

    fun updateGlowIntensity(intensity: Float) {
        val current = _config.value ?: return
        saveConfig(current.copy(glowIntensity = intensity.coerceIn(0f, 1f)))
    }

    fun updateTaskbarSettings(settings: TaskbarSettings) {
        val current = _config.value ?: return
        saveConfig(current.copy(taskbarSettings = settings))
    }
}