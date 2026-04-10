package com.nerf.launcher.state

import androidx.compose.runtime.Immutable
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.ui.reactor.ReactorCoreModel
import com.nerf.launcher.ui.reactor.ReactorDefaults
import com.nerf.launcher.ui.reactor.ReactorInteractionState
import com.nerf.launcher.ui.reactor.ReactorModel
import com.nerf.launcher.ui.reactor.ReactorSegmentModel
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.SystemModuleSnapshot

enum class LauncherMode(
    val displayName: String,
    val segmentId: String?,
    val accent: LauncherAccent,
    val summary: String,
    val detail: String,
    val glyph: String,
    val dockLabel: String
) {
    Hub(
        displayName = "PRIMARY HUB",
        segmentId = null,
        accent = LauncherAccent.Cyan,
        summary = "Central launcher nexus armed",
        detail = "Route all shell navigation through the nano-core hub.",
        glyph = "HUB",
        dockLabel = "Hub"
    ),
    Systems(
        displayName = "SYS / NET / DIAG",
        segmentId = "systems",
        accent = LauncherAccent.Cyan,
        summary = "Triad system lattice synchronized",
        detail = "System links, network pulse, and diagnostics remain mission-ready.",
        glyph = "SYS",
        dockLabel = "Systems"
    ),
    StabilityMonitor(
        displayName = "STABILITY MONITOR",
        segmentId = "stability_monitor",
        accent = LauncherAccent.Green,
        summary = "Core stability lock is holding",
        detail = "Thermal suppression, field pressure, and uptime variance remain stable.",
        glyph = "MON",
        dockLabel = "Stability"
    ),
    ReCalibration(
        displayName = "RE-CALIBRATION",
        segmentId = "re_calibration",
        accent = LauncherAccent.Yellow,
        summary = "Calibration queue awaiting dispatch",
        detail = "Re-trim launcher geometry, dock alignment, and field pacing from here.",
        glyph = "RCL",
        dockLabel = "Re-Cal"
    ),
    InterfaceConfig(
        displayName = "INTERFACE CONFIG",
        segmentId = "interface_config",
        accent = LauncherAccent.Magenta,
        summary = "Interface skin and control routing online",
        detail = "Update shell chrome, overlays, command bindings, and operator profiles.",
        glyph = "CFG",
        dockLabel = "Config"
    );

    companion object {
        fun fromSegmentId(segmentId: String): LauncherMode? =
            entries.firstOrNull { it.segmentId == segmentId }
    }
}

enum class LauncherUtilityAction(
    val label: String,
    val accent: LauncherAccent,
    val note: String
) {
    AppMatrix(
        label = "APPS",
        accent = LauncherAccent.Cyan,
        note = "Opening app matrix surface."
    ),
    LockGrid(
        label = "LOCK",
        accent = LauncherAccent.Magenta,
        note = "Chrome frame lock grid engaged."
    ),
    Assistant(
        label = "A.I.",
        accent = LauncherAccent.Green,
        note = "Opening assistant link."
    )
}

@Immutable
data class LauncherStatusModule(
    val id: String,
    val mode: LauncherMode,
    val title: String,
    val value: String,
    val detail: String,
    val footer: String,
    val accent: LauncherAccent
)

@Immutable
data class LauncherDockItem(
    val mode: LauncherMode,
    val label: String,
    val supportingText: String,
    val glyph: String,
    val accent: LauncherAccent
)

@Immutable
data class LauncherUiState(
    val headerTitle: String,
    val headerSubtitle: String,
    val headerEyebrow: String,
    val selectedMode: LauncherMode,
    val statusHeadline: String,
    val statusMessage: String,
    val reactor: ReactorModel,
    val reactorInteractionState: ReactorInteractionState,
    val statusModules: List<LauncherStatusModule>,
    val dockItems: List<LauncherDockItem>,
    val utilityActions: List<LauncherUtilityAction>,
    /** True for exactly one frame when the user taps the Assistant utility action. */
    val assistantRequested: Boolean = false,
    /** True for exactly one frame when the user taps the AppMatrix utility action. */
    val appDrawerRequested: Boolean = false
)

object LauncherUiStateFactory {

    /**
     * Build a full [LauncherUiState].
     *
     * Live data inputs:
     * @param config         Live [AppConfig] from [ConfigRepository] (theme, grid, taskbar, pinned apps).
     * @param telemetry      Live [SystemModuleSnapshot] from [SystemTelemetryRepository]
     *                       (battery, uptime, storage, power-save state).
     * @param transportLabel Active network transport resolved by [SystemTelemetryRepository].
     * @param wifiSignalLabel Wi-Fi signal quality string, or null if not on Wi-Fi.
     *
     * All parameters default to null/empty so that [create()] is safe to call
     * on the first frame before any live data has arrived.
     */
    fun create(
        selectedMode: LauncherMode = LauncherMode.Hub,
        interactionState: ReactorInteractionState = ReactorInteractionState(),
        statusMessage: String = selectedMode.summary,
        config: AppConfig? = null,
        telemetry: SystemModuleSnapshot? = null,
        transportLabel: String = "OFFLINE",
        wifiSignalLabel: String? = null
    ): LauncherUiState {
        return LauncherUiState(
            headerTitle    = "N.E.R.F. LAUNCHER",
            headerSubtitle = "Industrial command shell for reactor-first launcher control",
            headerEyebrow  = "HOME SECTOR",
            selectedMode   = selectedMode,
            statusHeadline = selectedMode.displayName,
            statusMessage  = statusMessage,
            reactor        = buildHomeReactor(selectedMode),
            reactorInteractionState = interactionState,
            statusModules  = buildModules(config, telemetry, transportLabel, wifiSignalLabel),
            dockItems      = buildDockItems(),
            utilityActions = LauncherUtilityAction.entries
        )
    }

    // ── Reactor ───────────────────────────────────────────────────────────────

    private fun buildHomeReactor(selectedMode: LauncherMode): ReactorModel {
        val activeSegmentIds = selectedMode.segmentId?.let(::setOf).orEmpty()
        return ReactorModel(
            segments = listOf(
                ReactorSegmentModel(
                    id       = LauncherMode.Systems.segmentId.orEmpty(),
                    label    = "SYS / NET / DIAG",
                    accent   = LauncherMode.Systems.accent,
                    isActive = LauncherMode.Systems.segmentId in activeSegmentIds
                ),
                ReactorSegmentModel(
                    id       = LauncherMode.StabilityMonitor.segmentId.orEmpty(),
                    label    = "STABILITY MONITOR",
                    accent   = LauncherMode.StabilityMonitor.accent,
                    isActive = LauncherMode.StabilityMonitor.segmentId in activeSegmentIds
                ),
                ReactorSegmentModel(
                    id       = LauncherMode.ReCalibration.segmentId.orEmpty(),
                    label    = "RE-CALIBRATION",
                    accent   = LauncherMode.ReCalibration.accent,
                    isActive = LauncherMode.ReCalibration.segmentId in activeSegmentIds
                ),
                ReactorSegmentModel(
                    id       = LauncherMode.InterfaceConfig.segmentId.orEmpty(),
                    label    = "INTERFACE CONFIG",
                    accent   = LauncherMode.InterfaceConfig.accent,
                    isActive = LauncherMode.InterfaceConfig.segmentId in activeSegmentIds
                )
            ),
            supportRings           = ReactorDefaults.supportRings(),
            core = ReactorCoreModel(
                title    = "N.E.R.F.",
                subtitle = "HUB",
                status   = if (selectedMode == LauncherMode.Hub) "PRIMARY" else "READY",
                accent   = selectedMode.accent,
                isOnline = true
            ),
            startAngle               = -132f,
            segmentGapAngle          = 10f,
            outerPaddingFraction     = 0.09f,
            outerRingThicknessFraction = 0.14f,
            labelRadiusFraction      = 0.68f,
            coreRadiusFraction       = 0.27f
        )
    }

    // ── Status modules ────────────────────────────────────────────────────────

    /**
     * Builds the four reactor status modules with live values where available.
     *
     * Module → Data source:
     *
     * [LauncherMode.Systems] — SYS / NET / DIAG
     *   value  : active transport type (Wi-Fi / Mobile Data / Ethernet / Offline) — LIVE
     *   footer : Wi-Fi signal quality string when on Wi-Fi; otherwise transport label — LIVE
     *
     * [LauncherMode.StabilityMonitor] — STABILITY MONITOR
     *   value  : battery percentage with charging indicator — LIVE
     *   footer : device uptime in days and hours — LIVE
     *
     * [LauncherMode.ReCalibration] — RE-CALIBRATION
     *   value  : taskbar enabled state from AppConfig — LIVE (config-driven)
     *   footer : storage usage percentage — LIVE
     *
     * [LauncherMode.InterfaceConfig] — INTERFACE CONFIG
     *   value  : active theme name from AppConfig — LIVE (config-driven)
     *   footer : grid size from AppConfig — LIVE (config-driven)
     */
    private fun buildModules(
        config: AppConfig?,
        snap: SystemModuleSnapshot?,
        transportLabel: String,
        wifiSignalLabel: String?
    ): List<LauncherStatusModule> {

        // ── Systems module: network connectivity ─────────────────────────────
        val networkValue  = transportLabel
        val networkFooter = wifiSignalLabel ?: transportLabel

        // ── Stability module: battery + uptime ───────────────────────────────
        val batteryValue = when {
            snap == null           -> "—"
            snap.batteryPercent == null -> "— %"
            snap.isCharging        -> "${snap.batteryPercent}% ⚡"
            else                   -> "${snap.batteryPercent}%"
        }
        val uptimeFooter = when {
            snap == null           -> "Uptime —"
            snap.uptimeDays > 0    -> "Up ${snap.uptimeDays}d ${snap.uptimeHours}h"
            else                   -> "Up ${snap.uptimeHours}h"
        }

        // ── ReCalibration module: taskbar + storage ─────────────────────────
        val taskbarState = when {
            config == null                   -> "—"
            config.taskbarSettings.enabled   -> "DOCK ON"
            else                             -> "DOCK OFF"
        }
        val storageFooter = when (snap?.storageUsagePercent) {
            null -> "Storage —"
            else -> "Storage ${snap.storageUsagePercent}% used"
        }

        // ── InterfaceConfig module: theme + grid (config-driven) ─────────────
        val themeName  = config?.themeName?.uppercase() ?: "—"
        val gridSize   = config?.gridSize?.toString() ?: "—"

        return listOf(
            LauncherStatusModule(
                id     = "module_systems",
                mode   = LauncherMode.Systems,
                title  = "SYSTEM TRIAD",
                value  = networkValue,
                detail = "SYS, NET, and DIAG channels remain phase-locked to the reactor wheel.",
                footer = networkFooter,
                accent = LauncherMode.Systems.accent
            ),
            LauncherStatusModule(
                id     = "module_stability",
                mode   = LauncherMode.StabilityMonitor,
                title  = "STABILITY MONITOR",
                value  = batteryValue,
                detail = "Thermal cage, pressure shell, and uptime harmonics are holding steady.",
                footer = uptimeFooter,
                accent = LauncherMode.StabilityMonitor.accent
            ),
            LauncherStatusModule(
                id     = "module_recal",
                mode   = LauncherMode.ReCalibration,
                title  = "RE-CALIBRATION",
                value  = taskbarState,
                detail = "Queued trim passes for dock geometry and shell pacing remain available.",
                footer = storageFooter,
                accent = LauncherMode.ReCalibration.accent
            ),
            LauncherStatusModule(
                id     = "module_config",
                mode   = LauncherMode.InterfaceConfig,
                title  = "INTERFACE CONFIG",
                value  = themeName,
                detail = "Overlay routing, command bindings, and chrome banks are synced.",
                footer = "Grid $gridSize × $gridSize",
                accent = LauncherMode.InterfaceConfig.accent
            )
        )
    }

    // ── Dock ──────────────────────────────────────────────────────────────────

    private fun buildDockItems(): List<LauncherDockItem> = LauncherMode.entries.map { mode ->
        LauncherDockItem(
            mode = mode,
            label = mode.dockLabel,
            supportingText = when (mode) {
                LauncherMode.Hub              -> "Primary"
                LauncherMode.Systems          -> "SYS/NET/DIAG"
                LauncherMode.StabilityMonitor -> "Shielding"
                LauncherMode.ReCalibration    -> "Trim Pass"
                LauncherMode.InterfaceConfig  -> "Profiles"
            },
            glyph  = mode.glyph,
            accent = mode.accent
        )
    }
}
