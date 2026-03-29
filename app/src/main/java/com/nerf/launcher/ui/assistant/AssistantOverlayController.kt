package com.nerf.launcher.ui.assistant

import android.view.View
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.nerf.launcher.R
import com.nerf.launcher.databinding.LayoutAssistantOverlayBinding
import com.nerf.launcher.util.assistant.AiResponseRepository
import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.util.assistant.AssistantState
import com.nerf.launcher.util.assistant.AssistantStateSnapshot

class AssistantOverlayController(
    private val binding: LayoutAssistantOverlayBinding,
    private val assistantController: AssistantController,
    private val onOpenSettings: () -> Unit,
    private val onOpenDiagnostics: () -> Unit,
    private val onOpenNodeHunter: () -> Unit,
    private val onShowLockSurface: () -> Unit
) {

    private var isVisible = false

    fun bind() {
        binding.assistantOverlayCloseButton.setOnClickListener { hide() }
        binding.assistantActionSettings.setOnClickListener {
            handleAction(
                action = {
                    assistantController.speakCustom(
                        binding.root.context.getString(R.string.assistant_overlay_action_settings_voice)
                    )
                    onOpenSettings()
                }
            )
        }
        binding.assistantActionDiagnostics.setOnClickListener {
            handleAction(
                action = {
                    assistantController.speakCategory(AiResponseRepository.Category.DIAGNOSTICS)
                    onOpenDiagnostics()
                }
            )
        }
        binding.assistantActionNodeHunter.setOnClickListener {
            handleAction(
                action = {
                    assistantController.speakCategory(AiResponseRepository.Category.NETWORK_SCAN)
                    onOpenNodeHunter()
                }
            )
        }
        binding.assistantActionLock.setOnClickListener {
            handleAction(
                action = {
                    assistantController.speakCustom(
                        binding.root.context.getString(R.string.assistant_overlay_action_lock_voice)
                    )
                    onShowLockSurface()
                }
            )
        }
        binding.assistantOverlayRoot.visibility = View.GONE
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
            AssistantState.MUTED,
            AssistantState.ERROR -> android.R.color.holo_red_light
        }
    }
}
