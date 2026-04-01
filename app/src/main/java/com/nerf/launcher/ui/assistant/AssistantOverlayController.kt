package com.nerf.launcher.ui.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
    private val assistantController: AssistantController,
    private val hasRecordAudioPermission: () -> Boolean,
    private val requestRecordAudioPermission: () -> Unit
) {

    private var isVisible = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isSpeechRecognitionAvailable = false
    private var isListening = false
    private var pendingMicStartAfterPermission = false

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
        binding.assistantOverlayMicButton.setOnClickListener { onMicButtonTapped() }
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
        initializeSpeechRecognizer()
        assistantController.onTranscriptChanged = ::renderTranscript
        renderState(assistantController.currentSnapshot())
        renderVoiceAvailability()
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
        stopVoiceRecognition()
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
        stopVoiceRecognition()
        speechRecognizer?.destroy()
        speechRecognizer = null
        assistantController.onTranscriptChanged = null
    }

    fun onRecordAudioPermissionResult(granted: Boolean) {
        if (granted && pendingMicStartAfterPermission) {
            pendingMicStartAfterPermission = false
            startVoiceRecognition()
            return
        }
        pendingMicStartAfterPermission = false
        if (!granted) {
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_permission_denied)
            binding.assistantOverlayStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_light)
            )
            binding.assistantOverlayResponse.text =
                binding.root.context.getString(R.string.assistant_overlay_voice_permission_denied_response)
        }
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

    private fun submitTypedCommand(clearInputAfterSubmit: Boolean = true) {
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
        if (clearInputAfterSubmit) {
            binding.assistantOverlayCommandInput.text?.clear()
        }
    }

    private fun submitRepeatLast() {
        binding.assistantOverlayCommandInput.setText("repeat")
        submitTypedCommand()
    }

    private fun interruptSpeaking() {
        assistantController.interruptSpeaking()
        stopVoiceRecognition()
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

    private fun initializeSpeechRecognizer() {
        val context = binding.root.context
        isSpeechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        if (!isSpeechRecognitionAvailable) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    assistantController.wakeForCommand()
                    binding.assistantOverlayStatus.text =
                        context.getString(R.string.assistant_overlay_status_listening)
                    binding.assistantOverlayStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.nerf_hud_lime)
                    )
                    binding.assistantOverlayResponse.text =
                        context.getString(R.string.assistant_overlay_voice_prompt_response)
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    isListening = false
                    binding.assistantOverlayStatus.text =
                        context.getString(R.string.assistant_overlay_status_thinking)
                    binding.assistantOverlayStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.nerf_hud_orange)
                    )
                }

                override fun onError(error: Int) {
                    isListening = false
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ) {
                        binding.assistantOverlayStatus.text =
                            context.getString(R.string.assistant_overlay_status_listening)
                        binding.assistantOverlayStatus.setTextColor(
                            ContextCompat.getColor(context, R.color.nerf_hud_lime)
                        )
                        binding.assistantOverlayResponse.text =
                            context.getString(R.string.assistant_overlay_voice_no_match_response)
                        return
                    }
                    binding.assistantOverlayStatus.text =
                        context.getString(R.string.assistant_overlay_status_error)
                    binding.assistantOverlayStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_red_light)
                    )
                    binding.assistantOverlayResponse.text =
                        context.getString(R.string.assistant_overlay_voice_error_response)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val transcript = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (transcript.isBlank()) {
                        binding.assistantOverlayResponse.text =
                            context.getString(R.string.assistant_overlay_voice_no_match_response)
                        return
                    }
                    binding.assistantOverlayCommandInput.setText(transcript)
                    submitTypedCommand(clearInputAfterSubmit = true)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        binding.assistantOverlayCommandInput.setText(partial)
                        binding.assistantOverlayCommandInput.setSelection(partial.length)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun renderVoiceAvailability() {
        binding.assistantOverlayMicButton.isEnabled = isSpeechRecognitionAvailable
        if (isSpeechRecognitionAvailable) return
        binding.assistantOverlayMicButton.text =
            binding.root.context.getString(R.string.assistant_overlay_voice_unavailable_button)
        binding.assistantOverlayResponse.text =
            binding.root.context.getString(R.string.assistant_overlay_voice_unavailable_response)
    }

    private fun onMicButtonTapped() {
        if (!isSpeechRecognitionAvailable) {
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_muted)
            binding.assistantOverlayStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_light)
            )
            return
        }

        if (isListening) {
            stopVoiceRecognition()
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_listening_stopped)
            binding.assistantOverlayStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.nerf_hud_cyan)
            )
            return
        }

        if (!hasRecordAudioPermission()) {
            pendingMicStartAfterPermission = true
            requestRecordAudioPermission()
            return
        }
        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        val recognizer = speechRecognizer ?: return
        val context = binding.root.context
        binding.assistantOverlayCommandInput.text?.clear()
        val recognizeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            recognizer.startListening(recognizeIntent)
        } catch (_: ActivityNotFoundException) {
            binding.assistantOverlayStatus.text =
                context.getString(R.string.assistant_overlay_status_error)
            binding.assistantOverlayResponse.text =
                context.getString(R.string.assistant_overlay_voice_unavailable_response)
        } catch (_: IllegalStateException) {
            binding.assistantOverlayStatus.text =
                context.getString(R.string.assistant_overlay_status_error)
            binding.assistantOverlayResponse.text =
                context.getString(R.string.assistant_overlay_voice_error_response)
        }
    }

    private fun stopVoiceRecognition() {
        isListening = false
        pendingMicStartAfterPermission = false
        speechRecognizer?.stopListening()
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
