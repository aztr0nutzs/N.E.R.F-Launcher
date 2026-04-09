package com.nerf.launcher.state

import androidx.compose.runtime.Immutable
import com.nerf.launcher.theme.LauncherAccent
import com.nerf.launcher.ui.reactor.ReactorCoreModel
import com.nerf.launcher.ui.reactor.ReactorDefaults
import com.nerf.launcher.ui.reactor.ReactorInteractionState
import com.nerf.launcher.ui.reactor.ReactorModel
import com.nerf.launcher.ui.reactor.ReactorSegmentModel
import com.nerf.launcher.util.AppConfig

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
    QuickScan(
        label = "SCAN",
        accent = LauncherAccent.Cyan,
        note = "Quick telemetry scan primed."
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
    val assistantRequested: Boolean = false
)

object LauncherUiStateFactory {

    /**
     * Build a full [LauncherUiState].
     *
     * @param config Optional live [AppConfig] from [ConfigRepository]. When present,
     *   config-driven fields (active theme name, grid size, pinned app count, taskbar
     *   state) are reflected in the status modules. When null (first frame before the
     *   repository is ready), sensible placeholder values are used — no crash, no
     *   separate code path.
     */
    fun create(
        selectedMode: LauncherMode = LauncherMode.Hub,
        interactionState: ReactorInteractionState = ReactorInteractionState(),
        statusMessage: String = selectedMode.summary,
        config: AppConfig? = null
    ): LauncherUiState {
        return LauncherUiState(
            headerTitle = "N.E.R.F. LAUNCHER",
            headerSubtitle = "Industrial command shell for reactor-first launcher control",
            headerEyebrow = "HOME SECTOR",
            selectedMode = selectedMode,
            statusHeadline = selectedMode.displayName,
            statusMessage = statusMessage,
            reactor = buildHomeReactor(selectedMode),
            reactorInteractionState = interactionState,
            statusModules = buildModules(config),
            dockItems = buildDockItems(),
            utilityActions = LauncherUtilityAction.entries
        )
    }

    private fun buildHomeReactor(selectedMode: LauncherMode): ReactorModel {
        val activeSegmentIds = selectedMode.segmentId?.let(::setOf).orEmpty()
        return ReactorModel(
            segments = listOf(
                ReactorSegmentModel(
                    id = LauncherMode.Systems.segmentId.orEmpty(),
                    label = "SYS / NET / DIAG",
                    accent = LauncherMode.Systems.accent,
                    isActive = LauncherMode.Systems.segmentId in activeSegmentIds
                ),
                ReactorSegmentModel(
                    id = LauncherMode.StabilityMonitor.segmentId.orEmpty(),
                    label = "STABILITY MONITOR",
                    accent = LauncherMode.StabilityMonitor.accent,
                    isActive = LauncherMode.StabilityMonitor.segmentId in activeSegmentIds
                ),
                ReactorSegmentModel(
                    id = LauncherMode.ReCalibration.segmentId.orEmpty(),
                    label = "RE-CALIBRATION",
                    accent = LauncherMode.ReCalibration.accent,
                    isActive = LauncherMode.ReCalibration.segmentId in activeSegmentIds
                ),
                ReactorSegmentModel(
                    id = LauncherMode.InterfaceConfig.segmentId.orEmpty(),
                    label = "INTERFACE CONFIG",
                    accent = LauncherMode.InterfaceConfig.accent,
                    isActive = LauncherMode.InterfaceConfig.segmentId in activeSegmentIds
                )
            ),
            supportRings = ReactorDefaults.supportRings(),
            core = ReactorCoreModel(
                title = "N.E.R.F.",
                subtitle = "HUB",
                status = if (selectedMode == LauncherMode.Hub) "PRIMARY" else "READY",
                accent = selectedMode.accent,
                isOnline = true
            ),
            startAngle = -132f,
            segmentGapAngle = 10f,
            outerPaddingFraction = 0.09f,
            outerRingThicknessFraction = 0.14f,
            labelRadiusFraction = 0.68f,
            coreRadiusFraction = 0.27f
        )
    }

    /**
     * Build the four status modules.
     *
     * Modules whose content is purely structural (Systems, StabilityMonitor) keep
     * their static display values — they represent launcher-mode identity, not live
     * device data.
     *
     * Modules whose content is user-configurable (ReCalibration, InterfaceConfig)
     * reflect real [AppConfig] values when available:
     *  - InterfaceConfig → active theme name, grid size
     *  - ReCalibration   → taskbar enabled state, pinned app count
     */
    private fun buildModules(config: AppConfig?): List<LauncherStatusModule> {
        // InterfaceConfig module: reflects active theme and grid size.
        val themeName   = config?.themeName ?: "—"
        val gridSize    = config?.gridSize?.toString() ?: "—"

        // ReCalibration module: reflects taskbar state and pinned-app count.
        val taskbarState = when {
            config == null               -> "—"
            config.taskbarSettings.enabled -> "DOCK ON"
            else                         -> "DOCK OFF"
        }
        val pinnedCount = config?.taskbarSettings?.pinnedApps?.size?.toString() ?: "—"

        return listOf(
            LauncherStatusModule(
                id      = "module_systems",
                mode    = LauncherMode.Systems,
                title   = "SYSTEM TRIAD",
                value   = "03 LINKS",
                detail  = "SYS, NET, and DIAG channels remain phase-locked to the reactor wheel.",
                footer  = "Latency 04 ms",
                accent  = LauncherMode.Systems.accent
            ),
            LauncherStatusModule(
                id      = "module_stability",
                mode    = LauncherMode.StabilityMonitor,
                title   = "STABILITY MONITOR",
                value   = "99.982%",
                detail  = "Thermal cage, pressure shell, and uptime harmonics are holding steady.",
                footer  = "Variance 0.018",
                accent  = LauncherMode.StabilityMonitor.accent
            ),
            LauncherStatusModule(
                id      = "module_recal",
                mode    = LauncherMode.ReCalibration,
                title   = "RE-CALIBRATION",
                value   = taskbarState,
                detail  = "Queued trim passes for dock geometry and shell pacing remain available.",
                footer  = "$pinnedCount pinned",
                accent  = LauncherMode.ReCalibration.accent
            ),
            LauncherStatusModule(
                id      = "module_config",
                mode    = LauncherMode.InterfaceConfig,
                title   = "INTERFACE CONFIG",
                value   = themeName.uppercase(),
                detail  = "Overlay routing, command bindings, and chrome banks are synced.",
                footer  = "Grid $gridSize × $gridSize",
                accent  = LauncherMode.InterfaceConfig.accent
            )
        )
    }

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
            glyph = mode.glyph,
            accent = mode.accent
        )
    }
}
