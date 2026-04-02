package com.nerf.launcher.util.assistant

import com.nerf.launcher.util.assistant.AiResponseRepository.Category

sealed class AssistantActionResult {
    data class CategoryResolved(
        val category: Category,
        val tags: Set<String> = emptySet()
    ) : AssistantActionResult()

    /**
     * Launcher command outcome contract:
     * - [LauncherOutcome.PERFORMED]: command executed and changed launcher state.
     * - [LauncherOutcome.ALREADY_ACTIVE]: no-op because requested state was already active.
     * - [LauncherOutcome.IN_PROGRESS]: no-op because equivalent work is already running.
     * - [LauncherOutcome.BLOCKED]: command cannot run now due to environment/state guardrails.
     * - [LauncherOutcome.UNSUPPORTED]: command is not wired/available in this build.
     * - [LauncherOutcome.FAILED]: command attempted but failed.
     * - [LauncherOutcome.INFORMATIONAL]: status/report command completed without mutating launcher state.
     */
    data class LauncherCommandHandled(
        val command: AssistantAction.LauncherCommand,
        val spokenText: String,
        val outcome: LauncherOutcome,
        val details: LauncherCommandDetails? = null
    ) : AssistantActionResult() {
        val performed: Boolean
            get() = outcome == LauncherOutcome.PERFORMED

        val alreadyActive: Boolean
            get() = outcome == LauncherOutcome.ALREADY_ACTIVE

        val inProgress: Boolean
            get() = outcome == LauncherOutcome.IN_PROGRESS

        val blocked: Boolean
            get() = outcome == LauncherOutcome.BLOCKED

        val unsupported: Boolean
            get() = outcome == LauncherOutcome.UNSUPPORTED

        val failed: Boolean
            get() = outcome == LauncherOutcome.FAILED

        val informational: Boolean
            get() = outcome == LauncherOutcome.INFORMATIONAL

        fun resolvedSpokenText(): String = when {
            spokenText.isNotBlank() -> spokenText
            performed -> "Done."
            alreadyActive -> "That is already active."
            inProgress -> "That process is already in progress."
            blocked -> "I can't do that right now."
            unsupported -> "That command is not supported on this build."
            failed -> "I tried, but that command failed."
            informational -> "No action was executed."
            else -> "No launcher action was executed."
        }
    }

    enum class LauncherOutcome {
        PERFORMED,
        ALREADY_ACTIVE,
        IN_PROGRESS,
        BLOCKED,
        UNSUPPORTED,
        FAILED,
        INFORMATIONAL
    }

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
