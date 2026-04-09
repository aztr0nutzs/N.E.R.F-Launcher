package com.nerf.launcher.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import com.nerf.launcher.theme.IndustrialLauncherColors
import com.nerf.launcher.theme.LauncherColors
import com.nerf.launcher.ui.reactor.ReactorInteractionState
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ConfigRepository

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    // ── UI state exposed to the Compose tree ─────────────────────────────────

    var uiState by mutableStateOf(LauncherUiStateFactory.create())
        private set

    /**
     * The [LauncherColors] palette to inject into [LauncherTheme].
     * Starts as the default industrial palette and updates whenever
     * [ConfigRepository] emits a new theme name.
     */
    var launcherColors by mutableStateOf(IndustrialLauncherColors)
        private set

    // ── Config observation ────────────────────────────────────────────────────

    private val configObserver = Observer<AppConfig> { config ->
        onConfigChanged(config)
    }

    init {
        // observeForever is safe here: we call removeObserver in onCleared().
        ConfigRepository.get().config.observeForever(configObserver)
    }

    override fun onCleared() {
        ConfigRepository.get().config.removeObserver(configObserver)
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

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Called on the main thread whenever [ConfigRepository] emits a new [AppConfig].
     * Converts config into Compose-friendly state without creating a duplicate authority:
     * - Theme name → [LauncherColors] palette via [LauncherColorsMapper]
     * - Config values that belong on visible UI modules → injected through [LauncherUiStateFactory]
     */
    private fun onConfigChanged(config: AppConfig) {
        launcherColors = LauncherColorsMapper.fromConfig(getApplication(), config)
        // Preserve the current interaction/navigation state; only update config-driven fields.
        rebuildFromConfig(config)
    }

    private fun rebuild(
        selectedMode: LauncherMode = uiState.selectedMode,
        interactionState: ReactorInteractionState = uiState.reactorInteractionState,
        statusMessage: String = uiState.statusMessage
    ) {
        val config = ConfigRepository.get().config.value
        uiState = LauncherUiStateFactory.create(
            selectedMode = selectedMode,
            interactionState = interactionState,
            statusMessage = statusMessage,
            config = config
        )
    }

    private fun rebuildFromConfig(config: AppConfig) {
        uiState = LauncherUiStateFactory.create(
            selectedMode = uiState.selectedMode,
            interactionState = uiState.reactorInteractionState,
            statusMessage = uiState.statusMessage,
            config = config
        )
    }
}
