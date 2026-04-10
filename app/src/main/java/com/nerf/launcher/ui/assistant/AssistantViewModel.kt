package com.nerf.launcher.ui.assistant

import android.app.Application
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.nerf.launcher.util.assistant.AssistantAction
import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.util.assistant.AssistantState
import com.nerf.launcher.util.assistant.AssistantStateSnapshot

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantViewModel
//
//  Bridges the existing AssistantController (text, voice, TTS, response bank)
//  to the Compose AssistantScreen. Exposes a single [uiState] snapshot and
//  handles [onEvent] dispatch.
// ─────────────────────────────────────────────────────────────────────────────

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = AssistantController(application)

    var uiState by mutableStateOf(AssistantUiState())
        private set

    private var activeThemeId: AssistantThemeId = AssistantThemeId.PHANTOM_BLACK

    init {
        val voiceAvailable = SpeechRecognizer.isRecognitionAvailable(application)
        val snapshot = controller.currentSnapshot()
        val bankLoaded = controller.isResponseBankLoaded()

        uiState = uiState.copy(
            robotState = snapshot.state,
            mood = snapshot.mood,
            latestResponse = snapshot.response ?: "",
            isVoiceAvailable = voiceAvailable,
            bankStatusLabel = if (bankLoaded) "RESPONSE BANK LOADED" else "RESPONSE BANK FALLBACK",
            interactionCount = snapshot.interactionCount,
            activeTheme = AssistantThemeRegistry.get(activeThemeId)
        )

        controller.onStateChanged = { snap -> handleStateChange(snap) }
        controller.onTranscriptChanged = { entries -> handleTranscriptChange(entries) }
        controller.onMoodChanged = { mood ->
            uiState = uiState.copy(mood = mood)
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun onEvent(event: AssistantEvent) {
        when (event) {
            is AssistantEvent.SubmitText          -> submitText(event.text)
            AssistantEvent.MicTapped              -> onMicTapped()
            AssistantEvent.RepeatLast             -> repeatLast()
            AssistantEvent.InterruptSpeaking      -> interruptSpeaking()

            // Input focus
            AssistantEvent.InputFocused           -> uiState = uiState.copy(isInputFocused = true)
            AssistantEvent.InputUnfocused         -> uiState = uiState.copy(isInputFocused = false)

            // Reactor
            AssistantEvent.ReactorCoreTapped      -> onReactorCoreTapped()
            is AssistantEvent.ReactorSectorTapped -> onReactorSectorTapped(event.sector)

            // Robot body
            AssistantEvent.ChestCoreTapped        -> onReactorCoreTapped()   // alias
            AssistantEvent.HandProjectionTapped   -> toggleHandProjection()
            is AssistantEvent.SideModuleTapped    -> toggleSideModule(event.side)

            // Left action stack
            is AssistantEvent.LeftActionTapped    -> onLeftActionTapped(event.action)

            // Dock
            is AssistantEvent.DockActionTapped    -> onDockActionTapped(event.action)
            AssistantEvent.DockCenterTapped       -> onDockCenterTapped()

            // Toggle module
            AssistantEvent.ToggleModuleTapped     -> uiState = uiState.copy(
                isToggleModuleOn = !uiState.isToggleModuleOn
            )

            // Quick launcher commands
            is AssistantEvent.LauncherCommandTapped -> executeLauncherCommand(event.command)

            // Theme
            is AssistantEvent.SwitchTheme         -> switchTheme(event.themeId)
            AssistantEvent.CycleTheme             -> cycleTheme()

            // Mood
            AssistantEvent.CycleMood              -> cycleMood()

            // Overlay
            AssistantEvent.Dismiss                -> dismiss()
            AssistantEvent.ToggleChatPane         -> toggleChatPane()
            is AssistantEvent.ToggleSidePanel     -> toggleSidePanelDirect(event.side)
        }
    }

    fun onInputTextChanged(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun show() {
        controller.wakeAssistant()
        uiState = uiState.copy(isVisible = true)
    }

    fun hide() {
        controller.setIdle()
        uiState = uiState.copy(isVisible = false)
    }

    // ── Internal Handlers ────────────────────────────────────────────────────

    private fun handleStateChange(snapshot: AssistantStateSnapshot) {
        uiState = uiState.copy(
            robotState = snapshot.state,
            mood = snapshot.mood,
            latestResponse = snapshot.response ?: uiState.latestResponse,
            interactionCount = snapshot.interactionCount,
            bankStatusLabel = if (controller.isResponseBankLoaded()) "RESPONSE BANK LOADED" else "RESPONSE BANK FALLBACK",
            // Auto-deactivate overlays when state returns to idle
            isChestCoreActive = if (snapshot.state == AssistantState.IDLE) false else uiState.isChestCoreActive,
            isHandProjectionActive = if (snapshot.state == AssistantState.IDLE) false else uiState.isHandProjectionActive,
            isReactorCoreBurst = false   // burst is one-shot; clear on state change
        )
    }

    private fun handleTranscriptChange(entries: List<AssistantController.TranscriptEntry>) {
        val messages = entries.map { entry ->
            TranscriptMessage(
                speaker = TranscriptMessage.Speaker.from(entry.speaker),
                text = entry.text,
                timestampMs = entry.timestampMs
            )
        }
        uiState = uiState.copy(transcript = messages)
    }

    private fun submitText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        controller.respondToInput(trimmed)
        uiState = uiState.copy(inputText = "")
    }

    private fun onMicTapped() {
        if (!uiState.isVoiceAvailable) return
        if (uiState.isListening) {
            uiState = uiState.copy(isListening = false)
            controller.interruptSpeaking()
        } else {
            uiState = uiState.copy(isListening = true)
            controller.wakeForCommand()
        }
    }

    private fun repeatLast() {
        controller.repeatLast()
    }

    private fun interruptSpeaking() {
        controller.interruptSpeaking()
        uiState = uiState.copy(isListening = false)
    }

    // ── Reactor ──────────────────────────────────────────────────────────────

    private fun onReactorCoreTapped() {
        val wasActive = uiState.isChestCoreActive
        uiState = uiState.copy(
            isChestCoreActive = !wasActive,
            isHandProjectionActive = false,     // mutual exclusion
            isReactorCoreBurst = !wasActive,    // one-shot FX burst on activation
            activeSector = null                  // deselect any sector
        )
        if (!wasActive) {
            controller.speakCategory(
                com.nerf.launcher.util.assistant.AiResponseRepository.Category.SCANNING
            )
        }
    }

    private fun onReactorSectorTapped(sector: ReactorSector) {
        val isAlreadyActive = uiState.activeSector == sector
        uiState = uiState.copy(
            activeSector = if (isAlreadyActive) null else sector,
            isChestCoreActive = false   // deactivate core when a sector is selected
        )
        if (!isAlreadyActive) {
            // Map reactor sector to a response category
            when (sector) {
                ReactorSector.STABILITY_MONITOR -> controller.speakCategory(
                    com.nerf.launcher.util.assistant.AiResponseRepository.Category.STATUS_REPORT
                )
                ReactorSector.INTERFACE_CONFIG -> controller.speakCategory(
                    com.nerf.launcher.util.assistant.AiResponseRepository.Category.THEME_SWITCH
                )
                ReactorSector.RECALIBRATION -> controller.speakCategory(
                    com.nerf.launcher.util.assistant.AiResponseRepository.Category.SCANNING
                )
                ReactorSector.SYS_NET_DIAG -> controller.speakCategory(
                    com.nerf.launcher.util.assistant.AiResponseRepository.Category.STATUS_REPORT
                )
            }
        }
    }

    // ── Hand Projection ───────────────────────────────────────────────────────

    private fun toggleHandProjection() {
        val wasActive = uiState.isHandProjectionActive
        uiState = uiState.copy(
            isHandProjectionActive = !wasActive,
            isChestCoreActive = false
        )
        if (!wasActive) {
            controller.speakCategory(
                com.nerf.launcher.util.assistant.AiResponseRepository.Category.STATUS_REPORT
            )
        }
    }

    // ── Side Modules ─────────────────────────────────────────────────────────

    private fun toggleSideModule(side: AssistantEvent.SideModuleTapped.Side) {
        when (side) {
            AssistantEvent.SideModuleTapped.Side.LEFT ->
                uiState = uiState.copy(isSidePanelLeftOpen = !uiState.isSidePanelLeftOpen)
            AssistantEvent.SideModuleTapped.Side.RIGHT ->
                uiState = uiState.copy(isSidePanelRightOpen = !uiState.isSidePanelRightOpen)
        }
    }

    private fun toggleSidePanelDirect(side: AssistantEvent.SideModuleTapped.Side) = toggleSideModule(side)

    // ── Left Action Stack ─────────────────────────────────────────────────────

    private fun onLeftActionTapped(action: LeftAction) {
        val isAlreadyActive = uiState.activeLeftAction == action
        uiState = uiState.copy(activeLeftAction = if (isAlreadyActive) null else action)
        when (action) {
            LeftAction.POWER    -> controller.speakCategory(
                com.nerf.launcher.util.assistant.AiResponseRepository.Category.STATUS_REPORT
            )
            LeftAction.NETWORK  -> controller.speakCategory(
                com.nerf.launcher.util.assistant.AiResponseRepository.Category.STATUS_REPORT
            )
            LeftAction.ALERTS   -> controller.speakCategory(
                com.nerf.launcher.util.assistant.AiResponseRepository.Category.SCANNING
            )
            LeftAction.SETTINGS -> executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_SETTINGS)
        }
    }

    // ── Dock ─────────────────────────────────────────────────────────────────

    private fun onDockActionTapped(action: DockAction) {
        val isAlreadyActive = uiState.activeDockAction == action
        uiState = uiState.copy(activeDockAction = if (isAlreadyActive) null else action)
        when (action) {
            DockAction.SETTINGS -> executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_SETTINGS)
            DockAction.MAP      -> executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_DIAGNOSTICS)
            DockAction.MODULES  -> executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_NODE_HUNTER)
            DockAction.MIC      -> onMicTapped()
            DockAction.PROFILE  -> controller.speakCategory(
                com.nerf.launcher.util.assistant.AiResponseRepository.Category.STATUS_REPORT
            )
        }
    }

    private fun onDockCenterTapped() {
        uiState = uiState.copy(isDockCenterActive = !uiState.isDockCenterActive)
        controller.wakeAssistant()
    }

    // ── Launcher Commands ─────────────────────────────────────────────────────

    private fun executeLauncherCommand(command: AssistantAction.LauncherCommand) {
        controller.executeLauncherCommand(command)
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private fun switchTheme(themeId: AssistantThemeId) {
        activeThemeId = themeId
        uiState = uiState.copy(activeTheme = AssistantThemeRegistry.get(themeId))
        controller.speakCategory(
            com.nerf.launcher.util.assistant.AiResponseRepository.Category.THEME_SWITCH
        )
    }

    private fun cycleTheme() {
        val next = AssistantThemeRegistry.next(activeThemeId)
        activeThemeId = next.id
        uiState = uiState.copy(activeTheme = next)
        controller.speakCategory(
            com.nerf.launcher.util.assistant.AiResponseRepository.Category.THEME_SWITCH
        )
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    private fun cycleMood() {
        controller.cycleMood()
    }

    private fun dismiss() {
        hide()
    }

    private fun toggleChatPane() {
        uiState = uiState.copy(isChatPaneOpen = !uiState.isChatPaneOpen)
    }

    override fun onCleared() {
        super.onCleared()
        controller.dispose()
    }
}
