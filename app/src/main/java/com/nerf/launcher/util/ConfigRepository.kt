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
        val themeName = PreferencesManager.getSelectedTheme(context) 
                ?: ThemeRepository.CLASSIC_NERF.name
        val iconPack = PreferencesManager.getIconPack(context) 
                ?: IconPackManager.DEFAULT_PACK
        val gridSize = PreferencesManager.getGridSize(context)
        val animationSpeedEnabled = PreferencesManager.isAnimationSpeedEnabled(context)
        val glowIntensity = PreferencesManager.getGlowIntensity(context)
        val taskbarSettings = TaskbarSettings(
            height = PreferencesManager.getTaskbarHeight(context),
            iconSize = PreferencesManager.getTaskbarIconSize(context),
            backgroundStyle = PreferencesManager.getTaskbarBackgroundStyle(context),
            transparency = PreferencesManager.getTaskbarTransparency(context),
            enabled = PreferencesManager.isTaskbarEnabled(context),
            pinnedApps = PreferencesManager.getPinnedApps(context)
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
        if (current.themeName == themeName) return
        saveConfig(current.copy(themeName = themeName))
    }

    fun updateIconPack(pack: String) {
        val current = _config.value ?: return
        if (current.iconPack == pack) return
        saveConfig(current.copy(iconPack = pack))
    }

    fun updateGridSize(size: Int) {
        val current = _config.value ?: return
        val coercedSize = size.coerceIn(2, 6)
        if (current.gridSize == coercedSize) return
        saveConfig(current.copy(gridSize = coercedSize))
    }

    fun updateAnimationSpeed(enabled: Boolean) {
        val current = _config.value ?: return
        if (current.animationSpeedEnabled == enabled) return
        saveConfig(current.copy(animationSpeedEnabled = enabled))
    }

    fun updateGlowIntensity(intensity: Float) {
        val current = _config.value ?: return
        val coercedIntensity = intensity.coerceIn(0f, 1f)
        if (current.glowIntensity == coercedIntensity) return
        saveConfig(current.copy(glowIntensity = coercedIntensity))
    }

    fun updateTaskbarSettings(settings: TaskbarSettings) {
        val current = _config.value ?: return
        if (current.taskbarSettings == settings) return
        saveConfig(current.copy(taskbarSettings = settings))
    }
}
