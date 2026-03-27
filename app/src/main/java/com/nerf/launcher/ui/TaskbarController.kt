package com.nerf.launcher.ui

import android.content.Context
import com.nerf.launcher.util.ConfigRepository

/**
 * Manages pinned apps for the taskbar.
 */
object TaskbarController {

    fun getPinnedApps(context: Context): List<String> {
        return ConfigRepository.get().config.value?.taskbarSettings?.pinnedApps.orEmpty()
    }

    fun savePinnedApps(context: Context, packages: List<String>) {
        val current = ConfigRepository.get().config.value ?: return
        ConfigRepository.get().updateTaskbarSettings(
            current.taskbarSettings.copy(pinnedApps = packages.distinct())
        )
    }

    fun addPinnedApp(context: Context, packageName: String) {
        val current = ConfigRepository.get().config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.toMutableList()
        if (!mutable.contains(packageName)) {
            mutable.add(packageName)
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    fun removePinnedApp(context: Context, packageName: String) {
        val current = ConfigRepository.get().config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.filterNot { it == packageName }
        if (mutable.size != current.taskbarSettings.pinnedApps.size) {
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    fun clearPinnedApps(context: Context) {
        val current = ConfigRepository.get().config.value ?: return
        if (current.taskbarSettings.pinnedApps.isNotEmpty()) {
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = emptyList())
            )
        }
    }
}
