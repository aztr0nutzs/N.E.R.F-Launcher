package com.nerf.launcher.util

import android.app.Application
import android.content.Context
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
                instance = ConfigRepository(app.applicationContext)
            }
        }

        fun get(): ConfigRepository =
            instance ?: error("ConfigRepository not initialized. Call init() in Application.")
    }

    private val _config = MutableLiveData(loadConfig())
    val config: LiveData<AppConfig> = _config

    private fun loadConfig(): AppConfig {
        return AppConfig(
            themeName = PreferencesManager.getSelectedTheme(context)
                ?: ThemeRepository.CLASSIC_NERF.name,
            iconPack = PreferencesManager.getIconPack(context) ?: IconPackManager.DEFAULT_PACK,
            gridSize = PreferencesManager.getGridSize(context),
            animationSpeedEnabled = PreferencesManager.isAnimationSpeedEnabled(context),
            glowIntensity = PreferencesManager.getGlowIntensity(context),
            taskbarSettings = TaskbarSettings(
                height = PreferencesManager.getTaskbarHeight(context),
                iconSize = PreferencesManager.getTaskbarIconSize(context),
                backgroundStyle = PreferencesManager.getTaskbarBackgroundStyle(context),
                transparency = PreferencesManager.getTaskbarTransparency(context),
                enabled = PreferencesManager.isTaskbarEnabled(context),
                pinnedApps = PreferencesManager.getPinnedApps(context)
            )
        )
    }

    fun saveConfig(config: AppConfig) {
        PreferencesManager.saveSelectedTheme(context, config.themeName)
        PreferencesManager.saveIconPack(context, config.iconPack)
        PreferencesManager.saveGridSize(context, config.gridSize)
        PreferencesManager.saveAnimationSpeed(context, config.animationSpeedEnabled)
        PreferencesManager.saveGlowIntensity(context, config.glowIntensity)

        PreferencesManager.saveTaskbarHeight(context, config.taskbarSettings.height)
        PreferencesManager.saveTaskbarIconSize(context, config.taskbarSettings.iconSize)
        PreferencesManager.saveTaskbarBackgroundStyle(context, config.taskbarSettings.backgroundStyle)
        PreferencesManager.saveTaskbarTransparency(context, config.taskbarSettings.transparency)
        PreferencesManager.saveTaskbarEnabled(context, config.taskbarSettings.enabled)
        PreferencesManager.savePinnedApps(context, config.taskbarSettings.pinnedApps)

        _config.value = config
    }

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
