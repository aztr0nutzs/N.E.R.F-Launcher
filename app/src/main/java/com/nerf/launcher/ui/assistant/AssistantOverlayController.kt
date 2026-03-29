package com.nerf.launcher.ui.assistant

import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.nerf.launcher.R
import com.nerf.launcher.databinding.LayoutAssistantOverlayBinding
import com.nerf.launcher.util.assistant.AiResponseRepository
import com.nerf.launcher.util.assistant.AssistantController

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
                status = binding.root.context.getString(R.string.assistant_overlay_action_settings_status),
                response = assistantController.speakCustom(
                    binding.root.context.getString(R.string.assistant_overlay_action_settings_voice)
                ),
                action = onOpenSettings
            )
        }
        binding.assistantActionDiagnostics.setOnClickListener {
            handleAction(
                status = binding.root.context.getString(R.string.assistant_overlay_action_diagnostics_status),
                response = assistantController.speakCategory(AiResponseRepository.Category.DIAGNOSTICS),
                action = onOpenDiagnostics
            )
        }
        binding.assistantActionNodeHunter.setOnClickListener {
            handleAction(
                status = binding.root.context.getString(R.string.assistant_overlay_action_node_hunter_status),
                response = assistantController.speakCategory(AiResponseRepository.Category.NETWORK_SCAN),
                action = onOpenNodeHunter
            )
        }
        binding.assistantActionLock.setOnClickListener {
            handleAction(
                status = binding.root.context.getString(R.string.assistant_overlay_action_lock_status),
                response = assistantController.speakCustom(
                    binding.root.context.getString(R.string.assistant_overlay_action_lock_voice)
                ),
                action = onShowLockSurface
            )
        }
        binding.assistantOverlayRoot.visibility = View.GONE
        binding.assistantOverlayBankState.text = bankStateLabel()
    }

    fun showWakeOverlay() {
        show()
        binding.assistantOverlayBankState.text = bankStateLabel()
        binding.assistantOverlayStatus.text = binding.root.context.getString(R.string.assistant_overlay_status_awake)
        binding.assistantOverlayResponse.text = assistantController.wakeAssistant()
    }

    fun hide() {
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

    private fun handleAction(status: String, response: String, action: () -> Unit) {
        binding.assistantOverlayStatus.text = status
        binding.assistantOverlayResponse.text = response
        hide()
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
}
