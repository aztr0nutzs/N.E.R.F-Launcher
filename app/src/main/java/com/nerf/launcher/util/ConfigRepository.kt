package com.nerf.launcher.util

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for launcher configuration.
 * Loads from and saves to SharedPreferences via PreferencesManager.
 * Exposes a StateFlow<AppConfig> that UI layers observe.
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

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private fun loadConfig(): AppConfig {
        val themeName = PreferencesManager.getSelectedTheme(context)
            ?.takeIf { ThemeRepository.containsTheme(it) }
            ?: ThemeRepository.defaultThemeName
        val iconPack = PreferencesManager.getIconPack(context)
            ?.takeIf { IconPackManager.getAvailablePacks(context).contains(it) }
            ?: IconPackManager.DEFAULT_PACK
        val taskbarSettings = normalizeTaskbarSettings(
            TaskbarSettings(
                height = PreferencesManager.getTaskbarHeight(context),
                iconSize = PreferencesManager.getTaskbarIconSize(context),
                backgroundStyle = PreferencesManager.getTaskbarBackgroundStyle(context),
                transparency = PreferencesManager.getTaskbarTransparency(context),
                enabled = PreferencesManager.isTaskbarEnabled(context),
                pinnedApps = PreferencesManager.getPinnedApps(context)
            )
        )
        return AppConfig(
            themeName = themeName,
            iconPack = iconPack,
            gridSize = PreferencesManager.getGridSize(context).coerceIn(2, 6),
            animationSpeedEnabled = PreferencesManager.isAnimationSpeedEnabled(context),
            glowIntensity = PreferencesManager.getGlowIntensity(context).coerceIn(0f, 1f),
            taskbarSettings = taskbarSettings
        )
    }

    fun saveConfig(config: AppConfig) {
        val sanitized = config.copy(
            themeName = config.themeName.takeIf { ThemeRepository.containsTheme(it) }
                ?: ThemeRepository.defaultThemeName,
            iconPack = config.iconPack.takeIf { IconPackManager.getAvailablePacks(context).contains(it) }
                ?: IconPackManager.DEFAULT_PACK,
            gridSize = config.gridSize.coerceIn(2, 6),
            glowIntensity = config.glowIntensity.coerceIn(0f, 1f),
            taskbarSettings = normalizeTaskbarSettings(config.taskbarSettings)
        )
        if (_config.value == sanitized) return

        PreferencesManager.saveSelectedTheme(context, sanitized.themeName)
        PreferencesManager.saveIconPack(context, sanitized.iconPack)
        PreferencesManager.saveGridSize(context, sanitized.gridSize)
        PreferencesManager.saveAnimationSpeed(context, sanitized.animationSpeedEnabled)
        PreferencesManager.saveGlowIntensity(context, sanitized.glowIntensity)

        PreferencesManager.saveTaskbarHeight(context, sanitized.taskbarSettings.height)
        PreferencesManager.saveTaskbarIconSize(context, sanitized.taskbarSettings.iconSize)
        PreferencesManager.saveTaskbarBackgroundStyle(context, sanitized.taskbarSettings.backgroundStyle)
        PreferencesManager.saveTaskbarTransparency(context, sanitized.taskbarSettings.transparency)
        PreferencesManager.saveTaskbarEnabled(context, sanitized.taskbarSettings.enabled)
        PreferencesManager.savePinnedApps(context, sanitized.taskbarSettings.pinnedApps)

        _config.value = sanitized
    }

    fun updateTheme(themeName: String) {
        saveConfig(_config.value.copy(themeName = themeName))
    }

    fun updateIconPack(pack: String) {
        saveConfig(_config.value.copy(iconPack = pack))
    }

    fun updateGridSize(size: Int) {
        saveConfig(_config.value.copy(gridSize = size.coerceIn(2, 6)))
    }

    fun updateAnimationSpeed(enabled: Boolean) {
        saveConfig(_config.value.copy(animationSpeedEnabled = enabled))
    }

    fun updateGlowIntensity(intensity: Float) {
        saveConfig(_config.value.copy(glowIntensity = intensity.coerceIn(0f, 1f)))
    }

    fun updateTaskbarSettings(settings: TaskbarSettings) {
        saveConfig(_config.value.copy(taskbarSettings = normalizeTaskbarSettings(settings)))
    }

    private fun normalizeTaskbarSettings(settings: TaskbarSettings): TaskbarSettings {
        val sanitizedBackground = if (settings.backgroundStyle in TaskbarSettings.supportedBackgroundStyles) {
            settings.backgroundStyle
        } else {
            TaskbarBackgroundStyle.default
        }
        return settings.copy(
            height = settings.height.coerceIn(40, 96),
            iconSize = settings.iconSize.coerceIn(24, 72),
            backgroundStyle = sanitizedBackground,
            transparency = settings.transparency.coerceIn(0f, 1f),
            pinnedApps = settings.pinnedApps
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()
        )
    }
}
