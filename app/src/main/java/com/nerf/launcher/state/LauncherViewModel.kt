package com.nerf.launcher.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.nerf.launcher.theme.IndustrialLauncherColors
import com.nerf.launcher.theme.LauncherColors
import com.nerf.launcher.ui.reactor.ReactorInteractionState
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.SystemModuleSnapshot
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    // ── UI state exposed to the Compose tree ─────────────────────────────────

    var uiState by mutableStateOf(LauncherUiStateFactory.create())
        private set

    /**
     * Live [LauncherColors] palette — updated whenever [ConfigRepository] emits
     * a new theme name or glow-intensity value. Injected into [LauncherTheme]
     * at the root by [NerfLauncherRoot].
     */
    var launcherColors by mutableStateOf(IndustrialLauncherColors)
        private set

    // ── Internal repositories ─────────────────────────────────────────────────

    private val telemetry = SystemTelemetryRepository(application)

    // ── Config observation ────────────────────────────────────────────────────

    private val configObserver = Observer<AppConfig> { config ->
        onConfigChanged(config)
    }

    init {
        // Config: observeForever is safe — we removeObserver in onCleared().
        ConfigRepository.get().config.observeForever(configObserver)

        // Telemetry: start the controller; collect its StateFlow on the IO dispatcher.
        telemetry.start()
        viewModelScope.launch {
            telemetry.snapshot.collect { snap ->
                if (snap != null) onTelemetryUpdated(snap)
            }
        }
    }

    override fun onCleared() {
        ConfigRepository.get().config.removeObserver(configObserver)
        telemetry.stop()
        super.onCleared()
    }

    // ── Interaction handlers ──────────────────────────────────────────────────

    fun selectMode(mode: LauncherMode) {
        rebuild(selectedMode = mode, statusMessage = mode.summary)
    }

    fun onDockItemTap(mode: LauncherMode) {
        rebuild(
            selectedMode = mode,
            statusMessage = "Dock transfer aligned to ${mode.displayName}."
        )
    }

    fun onUtilityActionTap(action: LauncherUtilityAction) {
        if (action == LauncherUtilityAction.Assistant) {
            uiState = uiState.copy(
                assistantRequested = true,
                statusMessage = action.note
            )
        } else {
            rebuild(statusMessage = action.note)
        }
    }

    /** Called by LauncherAppRoot after it has consumed the assistantRequested flag. */
    fun clearAssistantRequest() {
        if (uiState.assistantRequested) {
            uiState = uiState.copy(assistantRequested = false)
        }
    }

    fun onReactorCoreTap() {
        rebuild(
            selectedMode = LauncherMode.Hub,
            statusMessage = "Primary launcher hub synchronized to home sector."
        )
    }

    fun onReactorSegmentTap(segmentId: String) {
        val mode = LauncherMode.fromSegmentId(segmentId) ?: return
        rebuild(
            selectedMode = mode,
            statusMessage = "${mode.displayName} routed to the active reactor channel."
        )
    }

    fun onReactorSegmentLongPress(segmentId: String) {
        val mode = LauncherMode.fromSegmentId(segmentId) ?: return
        rebuild(
            selectedMode = mode,
            statusMessage = "${mode.displayName} held in extended control lock."
        )
    }

    fun onReactorInteractionStateChange(interactionState: ReactorInteractionState) {
        rebuild(interactionState = interactionState)
    }

    // ── Private update paths ──────────────────────────────────────────────────

    /**
     * Called on the main thread whenever [ConfigRepository] emits a new [AppConfig].
     * Converts the config into a new [LauncherColors] palette and rebuilds [uiState]
     * so config-driven module values (theme name, grid, taskbar) are refreshed.
     */
    private fun onConfigChanged(config: AppConfig) {
        launcherColors = LauncherColorsMapper.fromConfig(getApplication(), config)
        rebuildWithCurrentTelemetry(config = config)
    }

    /**
     * Called on the coroutine collector (main-default) whenever [SystemTelemetryRepository]
     * emits a fresh [SystemModuleSnapshot]. Rebuilds [uiState] so telemetry module values
     * (battery, uptime, storage, network) reflect the new data.
     */
    private fun onTelemetryUpdated(snap: SystemModuleSnapshot) {
        rebuildWithCurrentTelemetry(telemetry = snap)
    }

    private fun rebuild(
        selectedMode: LauncherMode = uiState.selectedMode,
        interactionState: ReactorInteractionState = uiState.reactorInteractionState,
        statusMessage: String = uiState.statusMessage
    ) {
        val config = ConfigRepository.get().config.value
        val snap   = telemetry.snapshot.value
        uiState = LauncherUiStateFactory.create(
            selectedMode     = selectedMode,
            interactionState = interactionState,
            statusMessage    = statusMessage,
            config           = config,
            telemetry        = snap,
            transportLabel   = telemetry.activeTransportLabel(),
            wifiSignalLabel  = telemetry.wifiSignalLabel()
        )
    }

    private fun rebuildWithCurrentTelemetry(
        config: AppConfig?   = ConfigRepository.get().config.value,
        telemetry: SystemModuleSnapshot? = this.telemetry.snapshot.value
    ) {
        uiState = LauncherUiStateFactory.create(
            selectedMode     = uiState.selectedMode,
            interactionState = uiState.reactorInteractionState,
            statusMessage    = uiState.statusMessage,
            config           = config,
            telemetry        = telemetry,
            transportLabel   = this.telemetry.activeTransportLabel(),
            wifiSignalLabel  = this.telemetry.wifiSignalLabel()
        )
    }
}
