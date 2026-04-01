package com.nerf.launcher.ui.assistant

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.nerf.launcher.R
import com.nerf.launcher.databinding.LayoutAssistantOverlayBinding
import com.nerf.launcher.util.assistant.AssistantAction
import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.util.assistant.AssistantState
import com.nerf.launcher.util.assistant.AssistantStateSnapshot

class AssistantOverlayController(
    private val binding: LayoutAssistantOverlayBinding,
    private val assistantController: AssistantController
) {

    private var isVisible = false

    fun bind() {
        binding.assistantOverlayCloseButton.setOnClickListener { hide() }
        binding.assistantActionSettings.setOnClickListener {
            handleAction(
                action = {
                    assistantController.executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_SETTINGS)
                }
            )
        }
        binding.assistantActionDiagnostics.setOnClickListener {
            handleAction(
                action = {
                    assistantController.executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_DIAGNOSTICS)
                }
            )
        }
        binding.assistantActionNodeHunter.setOnClickListener {
            handleAction(
                action = {
                    assistantController.executeLauncherCommand(AssistantAction.LauncherCommand.OPEN_NODE_HUNTER)
                }
            )
        }
        binding.assistantActionLock.setOnClickListener {
            handleAction(
                action = {
                    assistantController.executeLauncherCommand(AssistantAction.LauncherCommand.SHOW_LOCK_SURFACE)
                }
            )
        }
        binding.assistantOverlaySubmitButton.setOnClickListener { submitTypedCommand() }
        binding.assistantOverlayRepeatLastButton.setOnClickListener { submitRepeatLast() }
        binding.assistantOverlayInterruptButton.setOnClickListener { interruptSpeaking() }
        binding.assistantOverlayCommandInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitTypedCommand()
                true
            } else {
                false
            }
        }
        binding.assistantOverlayRoot.visibility = View.GONE
        assistantController.onTranscriptChanged = ::renderTranscript
        renderState(assistantController.currentSnapshot())
    }

    fun showWakeOverlay() {
        show()
        assistantController.wakeAssistant()
    }

    fun wakeForCoreAction() {
        assistantController.wakeForCommand()
    }

    fun hide() {
        hide(resetAssistantToIdle = true)
    }

    fun renderState(snapshot: AssistantStateSnapshot) {
        binding.assistantOverlayBankState.text = bankStateLabel()
        binding.assistantOverlayMoodIndicator.text = binding.root.context.getString(
            R.string.assistant_overlay_mood_indicator_format,
            snapshot.mood.label.uppercase()
        )
        binding.assistantOverlayStatus.text = stateLabel(snapshot.state)
        binding.assistantOverlayStatus.setTextColor(
            ContextCompat.getColor(binding.root.context, stateColor(snapshot.state))
        )
        when {
            snapshot.response != null -> binding.assistantOverlayResponse.text = snapshot.response
            snapshot.state == AssistantState.IDLE -> {
                binding.assistantOverlayResponse.text =
                    binding.root.context.getString(R.string.assistant_overlay_response_idle)
            }
        }
    }

    private fun hide(resetAssistantToIdle: Boolean) {
        if (!isVisible) return
        isVisible = false
        binding.assistantOverlayCard.animate()
            .alpha(0f)
            .translationY(-12f)
            .setDuration(150L)
            .setInterpolator(LinearOutSlowInInterpolator())
            .withEndAction {
                binding.assistantOverlayRoot.visibility = View.GONE
            }
            .start()
        if (resetAssistantToIdle) {
            assistantController.setIdle()
        }
    }

    fun isShowing(): Boolean = isVisible

    fun release() {
        assistantController.onTranscriptChanged = null
        assistantController.shutdown()
    }

    private fun show() {
        if (isVisible) return
        isVisible = true
        binding.assistantOverlayRoot.visibility = View.VISIBLE
        binding.assistantOverlayCard.alpha = 0f
        binding.assistantOverlayCard.translationY = -12f
        binding.assistantOverlayCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180L)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun handleAction(action: () -> Unit) {
        hide(resetAssistantToIdle = false)
        action()
    }

    private fun submitTypedCommand() {
        val typedInput = binding.assistantOverlayCommandInput.text?.toString().orEmpty()
        val trimmedInput = typedInput.trim()

        if (trimmedInput.isEmpty()) {
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_listening)
            binding.assistantOverlayStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.nerf_hud_lime)
            )
            binding.assistantOverlayResponse.text =
                binding.root.context.getString(R.string.assistant_overlay_response_empty_command)
            return
        }

        val response = assistantController.respondToInput(trimmedInput)
        if (response.isNullOrBlank()) {
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_error)
            binding.assistantOverlayStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_light)
            )
            binding.assistantOverlayResponse.text =
                binding.root.context.getString(R.string.assistant_overlay_response_unknown_command)
        } else {
            binding.assistantOverlayResponse.text = response
        }
        binding.assistantOverlayCommandInput.text?.clear()
    }

    private fun submitRepeatLast() {
        binding.assistantOverlayCommandInput.setText("repeat")
        submitTypedCommand()
    }

    private fun interruptSpeaking() {
        assistantController.interruptSpeaking()
        binding.assistantOverlayStatus.text =
            binding.root.context.getString(R.string.assistant_overlay_status_listening)
        binding.assistantOverlayStatus.setTextColor(
            ContextCompat.getColor(binding.root.context, R.color.nerf_hud_lime)
        )
    }

    private fun renderTranscript(entries: List<AssistantController.TranscriptEntry>) {
        if (entries.isEmpty()) {
            binding.assistantOverlayTranscript.text =
                binding.root.context.getString(R.string.assistant_overlay_transcript_empty)
            return
        }
        val context = binding.root.context
        binding.assistantOverlayTranscript.text = entries.asReversed().joinToString("\n") { entry ->
            val prefix = when (entry.speaker) {
                AssistantController.Speaker.USER -> context.getString(R.string.assistant_overlay_transcript_user_prefix)
                AssistantController.Speaker.ASSISTANT -> context.getString(R.string.assistant_overlay_transcript_assistant_prefix)
            }
            "$prefix: ${entry.text}"
        }
        binding.assistantOverlayTranscriptContainer.post {
            binding.assistantOverlayTranscriptContainer.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun bankStateLabel(): String {
        val stringRes = if (assistantController.isResponseBankLoaded()) {
            R.string.assistant_overlay_bank_loaded
        } else {
            R.string.assistant_overlay_bank_fallback
        }
        return binding.root.context.getString(stringRes)
    }

    private fun stateLabel(state: AssistantState): String {
        val stringRes = when (state) {
            AssistantState.IDLE -> R.string.assistant_overlay_status_idle
            AssistantState.WAKE -> R.string.assistant_overlay_status_wake
            AssistantState.LISTENING -> R.string.assistant_overlay_status_listening
            AssistantState.THINKING -> R.string.assistant_overlay_status_thinking
            AssistantState.RESPONDING -> R.string.assistant_overlay_status_responding
            AssistantState.SPEAKING -> R.string.assistant_overlay_status_speaking
            AssistantState.MUTED -> R.string.assistant_overlay_status_muted
            AssistantState.ERROR -> R.string.assistant_overlay_status_error
            AssistantState.PROCESSING -> R.string.assistant_overlay_status_thinking
            AssistantState.AWAITING_INPUT -> R.string.assistant_overlay_status_listening
            AssistantState.COOLING_DOWN -> R.string.assistant_overlay_status_idle
            AssistantState.REBOOTING -> R.string.assistant_overlay_status_wake
            AssistantState.SHUTTING_DOWN -> R.string.assistant_overlay_status_muted
        }
        return binding.root.context.getString(stringRes)
    }

    private fun stateColor(state: AssistantState): Int {
        return when (state) {
            AssistantState.IDLE -> R.color.nerf_hud_cyan
            AssistantState.WAKE -> R.color.nerf_hud_orange
            AssistantState.LISTENING -> R.color.nerf_hud_lime
            AssistantState.THINKING -> R.color.nerf_hud_orange
            AssistantState.RESPONDING -> R.color.nerf_hud_cyan
            AssistantState.SPEAKING -> R.color.nerf_hud_magenta
            AssistantState.PROCESSING -> R.color.nerf_hud_orange
            AssistantState.AWAITING_INPUT -> R.color.nerf_hud_lime
            AssistantState.COOLING_DOWN -> R.color.nerf_hud_cyan
            AssistantState.REBOOTING -> R.color.nerf_hud_orange
            AssistantState.MUTED,
            AssistantState.ERROR,
            AssistantState.SHUTTING_DOWN -> android.R.color.holo_red_light
        }
    }
}
