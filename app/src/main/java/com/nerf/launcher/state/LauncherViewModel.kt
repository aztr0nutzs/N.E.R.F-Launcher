package com.nerf.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nerf.launcher.ui.reactor.ReactorInteractionState

class LauncherViewModel : ViewModel() {
    var uiState by mutableStateOf(LauncherUiStateFactory.create())
        private set

    fun selectMode(mode: LauncherMode) {
        rebuild(
            selectedMode = mode,
            statusMessage = mode.summary
        )
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

    private fun rebuild(
        selectedMode: LauncherMode = uiState.selectedMode,
        interactionState: ReactorInteractionState = uiState.reactorInteractionState,
        statusMessage: String = uiState.statusMessage
    ) {
        uiState = LauncherUiStateFactory.create(
            selectedMode = selectedMode,
            interactionState = interactionState,
            statusMessage = statusMessage
        )
    }
}
