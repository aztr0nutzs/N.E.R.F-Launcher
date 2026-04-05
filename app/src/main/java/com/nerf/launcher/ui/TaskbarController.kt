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
        val repository = ConfigRepository.get()
        val current = repository.config.value ?: return
        repository.updateTaskbarSettings(
            current.taskbarSettings.copy(pinnedApps = packages.distinct())
        )
    }

    fun addPinnedApp(packageName: String) {
        val repository = ConfigRepository.get()
        val current = repository.config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.toMutableList()
        if (!mutable.contains(packageName)) {
            mutable.add(packageName)
            repository.updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }

    fun removePinnedApp(packageName: String) {
        val repository = ConfigRepository.get()
        val current = repository.config.value ?: return
        val mutable = current.taskbarSettings.pinnedApps.filterNot { it == packageName }
        if (mutable.size != current.taskbarSettings.pinnedApps.size) {
            repository.updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = mutable)
            )
        }
    }


    fun isPinned(packageName: String): Boolean {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return false
        return ConfigRepository.get().config.value
            ?.taskbarSettings
            ?.pinnedApps
            ?.contains(normalized) == true
    }

    fun togglePinnedApp(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return
        if (isPinned(normalized)) {
            removePinnedApp(normalized)
        } else {
            addPinnedApp(normalized)
        }
    }

    fun clearPinnedApps() {
        val repository = ConfigRepository.get()
        val current = repository.config.value ?: return
        if (current.taskbarSettings.pinnedApps.isNotEmpty()) {
            repository.updateTaskbarSettings(
                current.taskbarSettings.copy(pinnedApps = emptyList())
            )
        }
    }

    fun updateSettings(transform: TaskbarSettings.() -> TaskbarSettings) {
        val repository = ConfigRepository.get()
        val current = repository.config.value ?: return
        repository.updateTaskbarSettings(current.taskbarSettings.transform())
    }
}
