package com.nerf.launcher.ui

import android.content.Context
import com.nerf.launcher.util.ConfigRepository

/**
 * Manages the state of the taskbar, including pinned app packages.
 * Reads configuration exclusively from ConfigRepository (single source of truth).
 */
object TaskbarController {
    /** Returns the list of pinned app package names from ConfigRepository. */
    fun getPinnedApps(context: Context): List<String> =
        ConfigRepository.get().config.value?.taskbarSettings?.pinnedApps ?: emptyList()

    /** Saves the list of pinned app package names to ConfigRepository. */
    fun savePinnedApps(context: Context, packages: List<String>) {
        val current = ConfigRepository.get().config.value ?: return
        val updatedSettings = current.taskbarSettings.copy(pinnedApps = packages)
        ConfigRepository.get().updateTaskbarSettings(updatedSettings)
    }

    /** Adds a package to the pinned apps list (FIFO limited to 4). */
    fun addPinnedApp(context: Context, packageName: String) {
        val current = ConfigRepository.get().config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.toMutableList()
        if (!mutable.contains(packageName)) {
            mutable.add(packageName)
            if (mutable.size > 4) {
                mutable.removeAt(0) // Remove oldest
            }
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    /** Removes a package from the pinned apps list. */
    fun removePinnedApp(context: Context, packageName: String) {
        val current = ConfigRepository.get().config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.toMutableList()
        mutable.removeAll { it == packageName }
        if (mutable.size != current.taskbarSettings.pinnedApps.size) {
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    /** Clears all pinned apps. */
    fun clearPinnedApps(context: Context) {
        val current = ConfigRepository.get().config.value ?: return
        if (current.taskbarSettings.pinnedApps.isNotEmpty()) {
            ConfigRepository.get().updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = emptyList())
            )
        }
    }
}