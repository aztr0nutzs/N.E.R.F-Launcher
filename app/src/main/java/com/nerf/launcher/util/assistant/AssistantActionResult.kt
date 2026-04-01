package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

sealed class AssistantActionResult {
    data class CategoryResolved(
        val category: Category,
        val tags: Set<String> = emptySet()
    ) : AssistantActionResult()

    data class LauncherCommandHandled(
        val command: AssistantAction.LauncherCommand,
        val spokenText: String,
        val performed: Boolean,
        val details: LauncherCommandDetails? = null
    ) : AssistantActionResult()

    data object RepeatLast : AssistantActionResult()

    data object Ignored : AssistantActionResult()

    sealed class LauncherCommandDetails {
        data class OpenedDestination(
            val destination: String
        ) : LauncherCommandDetails()

        data class ThemeCycled(
            val previousTheme: String,
            val newTheme: String
        ) : LauncherCommandDetails()

        data class CurrentTheme(
            val activeTheme: String?
        ) : LauncherCommandDetails()

        data class SystemState(
            val batteryPercent: Int?,
            val storageUsagePercent: Int?,
            val uptimeDays: Long,
            val uptimeHours: Long,
            val isPowerSaveMode: Boolean
        ) : LauncherCommandDetails()

        data class AppFilterState(
            val totalApps: Int,
            val filteredApps: Int
        ) : LauncherCommandDetails()

        data class NetworkScanStatus(
            val supported: Boolean,
            val running: Boolean,
            val nodeCount: Int?
        ) : LauncherCommandDetails()

        data class NetworkScanSummary(
            val nodeCount: Int,
            val averagePingMs: Long?,
            val fastestPingMs: Long?,
            val slowestPingMs: Long?
        ) : LauncherCommandDetails()
    }
}
