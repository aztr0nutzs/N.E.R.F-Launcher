package com.nerf.launcher.ui

import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.TaskbarSettings

/**
 * Manages pinned apps for the taskbar.
 */
object TaskbarController {

    fun getPinnedApps(): List<String> {
        return ConfigRepository.get().config.value?.taskbarSettings?.pinnedApps.orEmpty()
    }

    fun savePinnedApps(packages: List<String>) {
        val current = ConfigRepository.get().config.value ?: return
        ConfigRepository.get().updateTaskbarSettings(
            current.taskbarSettings.copy(pinnedApps = packages.distinct())
        )
    }

    fun addPinnedApp(packageName: String) {
        val current = ConfigRepository.get().config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.toMutableList()
        if (!mutable.contains(packageName)) {
            mutable.add(packageName)
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    fun removePinnedApp(packageName: String) {
        val current = ConfigRepository.get().config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.filterNot { it == packageName }
        if (mutable.size != current.taskbarSettings.pinnedApps.size) {
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    fun clearPinnedApps() {
        val current = ConfigRepository.get().config.value ?: return
        if (current.taskbarSettings.pinnedApps.isNotEmpty()) {
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = emptyList())
            )
        }
    }

    fun updateSettings(transform: TaskbarSettings.() -> TaskbarSettings) {
        val current = ConfigRepository.get().config.value ?: return
        ConfigRepository.get().updateTaskbarSettings(current.taskbarSettings.transform())
    }
}
