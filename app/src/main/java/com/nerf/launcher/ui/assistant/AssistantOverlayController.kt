package com.nerf.launcher.ui.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.LifecycleOwner
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.databinding.LayoutAssistantOverlayBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.NerfTheme
import com.nerf.launcher.util.ThemeManager
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
    private var currentState: AssistantState = AssistantState.IDLE
    private var activeTheme: NerfTheme? = null
    private var lastThemeKey: Pair<String, Float>? = null

    fun bind(lifecycleOwner: LifecycleOwner) {
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
        applyInitialTheme()
        observeTheme(lifecycleOwner)
        initializeSpeechRecognizer()
        assistantController.onTranscriptChanged = ::renderTranscript
        renderState(assistantController.currentSnapshot())
        renderVoiceAvailability()
        configureVideoLoop()
        startIdleVisualLoop()
    }

    private fun applyInitialTheme() {
        val config = ConfigRepository.get().config.value
        val theme = if (config != null) {
            lastThemeKey = ThemeManager.themeKey(config)
            ThemeManager.resolveConfigTheme(binding.root.context, config)
        } else {
            ThemeManager.resolveActiveTheme(binding.root.context)
        }
        applyTheme(theme)
    }

    private fun observeTheme(lifecycleOwner: LifecycleOwner) {
        ConfigRepository.get().config.observe(lifecycleOwner) { config ->
            val themeKey = ThemeManager.themeKey(config)
            if (themeKey == lastThemeKey) return@observe
            val theme = ThemeManager.resolveConfigTheme(binding.root.context, config)
            applyTheme(theme)
            lastThemeKey = themeKey
        }
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
        currentState = snapshot.state
        binding.assistantOverlayBankState.text = bankStateLabel()
        binding.assistantOverlayMoodIndicator.text = binding.root.context.getString(
            R.string.assistant_overlay_mood_indicator_format,
            snapshot.mood.label.uppercase()
        )
        binding.assistantOverlayStatus.text = stateLabel(snapshot.state)
        binding.assistantOverlayStatus.setTextColor(stateColor(snapshot.state))
        when {
            snapshot.response != null -> binding.assistantOverlayResponse.text = snapshot.response
            snapshot.state == AssistantState.IDLE -> {
                binding.assistantOverlayResponse.text =
                    binding.root.context.getString(R.string.assistant_overlay_response_idle)
            }
        }
        renderVisualState(snapshot.state)
    }

    private fun hide(resetAssistantToIdle: Boolean) {
        if (!isVisible) return
        isVisible = false
        stopVoiceRecognition()
        stopIdleVisualLoop()
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
        stopIdleVisualLoop()
        binding.assistantOverlayVisualCore.animate().cancel()
        binding.assistantOverlayListeningIndicator.animate().cancel()
        binding.assistantOverlaySpeakingIndicator.animate().cancel()
        binding.assistantOverlayVisualVideo.stopPlayback()
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
            binding.assistantOverlayStatus.setTextColor(assistantErrorColor())
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
        startIdleVisualLoop()
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
            binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.LISTENING))
            binding.assistantOverlayResponse.text =
                binding.root.context.getString(R.string.assistant_overlay_response_empty_command)
            return
        }

        val response = assistantController.respondToInput(trimmedInput)
        if (response.isNullOrBlank()) {
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_error)
            binding.assistantOverlayStatus.setTextColor(assistantErrorColor())
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
        binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.LISTENING))
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
                    binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.LISTENING))
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
                    binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.THINKING))
                }

                override fun onError(error: Int) {
                    isListening = false
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ) {
                        binding.assistantOverlayStatus.text =
                            context.getString(R.string.assistant_overlay_status_listening)
                        binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.LISTENING))
                        binding.assistantOverlayResponse.text =
                            context.getString(R.string.assistant_overlay_voice_no_match_response)
                        return
                    }
                    binding.assistantOverlayStatus.text =
                        context.getString(R.string.assistant_overlay_status_error)
                    binding.assistantOverlayStatus.setTextColor(assistantErrorColor())
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
        binding.assistantOverlayModeIndicator.text = if (isSpeechRecognitionAvailable) {
            binding.root.context.getString(R.string.assistant_overlay_mode_voice)
        } else {
            binding.root.context.getString(R.string.assistant_overlay_mode_text)
        }
        binding.assistantOverlayModeIndicator.setTextColor(
            if (isSpeechRecognitionAvailable) {
                stateColor(AssistantState.LISTENING)
            } else {
                stateColor(AssistantState.THINKING)
            }
        )

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
            binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.MUTED))
            return
        }

        if (isListening) {
            stopVoiceRecognition()
            binding.assistantOverlayStatus.text =
                binding.root.context.getString(R.string.assistant_overlay_status_listening_stopped)
            binding.assistantOverlayStatus.setTextColor(stateColor(AssistantState.IDLE))
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
            renderVisualState(AssistantState.LISTENING)
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
            AssistantState.REBOOTING -> R.string.assistant_overlay_status_wake
            AssistantState.SHUTTING_DOWN -> R.string.assistant_overlay_status_muted
        }
        return binding.root.context.getString(stringRes)
    }

    private fun stateColor(state: AssistantState): Int {
        val theme = currentTheme()
        return when (state) {
            AssistantState.IDLE -> theme.hudInfoColor
            AssistantState.WAKE -> theme.hudWarningColor
            AssistantState.LISTENING -> theme.hudSuccessColor
            AssistantState.THINKING -> theme.hudWarningColor
            AssistantState.RESPONDING -> theme.hudInfoColor
            AssistantState.SPEAKING -> theme.hudAccentColor
            AssistantState.REBOOTING -> theme.hudWarningColor
            AssistantState.MUTED -> theme.assistantMutedColor
            AssistantState.ERROR -> theme.assistantErrorColor
            AssistantState.SHUTTING_DOWN -> theme.assistantErrorColor
        }
    }

    fun applyTheme(theme: NerfTheme) {
        activeTheme = theme
        binding.assistantOverlayTitle.setTextColor(theme.hudAccentColor)
        binding.assistantOverlayBankState.setTextColor(theme.hudPanelTextSecondary)
        binding.assistantOverlayVisualLabel.setTextColor(theme.hudInfoColor)
        binding.assistantOverlayVisualHint.setTextColor(theme.hudPanelTextPrimary)
        binding.assistantOverlayMoodIndicator.setTextColor(theme.hudAccentColor)
        binding.assistantOverlayResponse.setTextColor(theme.hudPanelTextPrimary)
        binding.assistantOverlayTranscriptLabel.setTextColor(theme.hudWarningColor)
        binding.assistantOverlayTranscript.setTextColor(theme.hudPanelTextPrimary)
        binding.assistantOverlayActionsLabel.setTextColor(theme.hudSuccessColor)
        binding.assistantOverlayCommandInput.setTextColor(theme.hudPanelTextPrimary)
        binding.assistantOverlayCommandInput.setHintTextColor(theme.hudPanelTextSecondary)
        binding.assistantOverlayStatus.setTextColor(stateColor(currentState))

        tintBackground(
            binding.assistantOverlayCard,
            ColorUtils.setAlphaComponent(theme.reactorInteriorDarkColor, 0xF2)
        )
        val shellTint = ColorUtils.setAlphaComponent(theme.windowBackground, 0xD6)
        tintBackground(binding.assistantOverlayResponse, shellTint)
        tintBackground(binding.assistantOverlayTranscriptContainer, shellTint)
        tintBackground(binding.assistantOverlayCommandInput, shellTint)
        tintBackground(binding.assistantOverlayVisualCoreContainer, shellTint)

        setButtonStyle(binding.assistantOverlayCloseButton, theme.hudWarningColor)
        setButtonStyle(binding.assistantOverlaySubmitButton, theme.hudInfoColor)
        setButtonStyle(binding.assistantOverlayMicButton, theme.hudSuccessColor)
        setButtonStyle(binding.assistantOverlayRepeatLastButton, theme.hudAccentColor)
        setButtonStyle(binding.assistantOverlayInterruptButton, theme.hudWarningColor)
        setButtonStyle(binding.assistantActionSettings, theme.hudInfoColor)
        setButtonStyle(binding.assistantActionDiagnostics, theme.hudWarningColor)
        setButtonStyle(binding.assistantActionNodeHunter, theme.hudSuccessColor)
        setButtonStyle(binding.assistantActionLock, theme.hudAccentColor)

        renderVoiceAvailability()
        renderVisualState(currentState)
    }

    private fun renderVisualState(state: AssistantState) {
        val tintColor = stateColor(state)
        binding.assistantOverlayListeningIndicator.background.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        binding.assistantOverlaySpeakingIndicator.background.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)

        binding.assistantOverlayListeningIndicator.visibility = if (state == AssistantState.LISTENING) View.VISIBLE else View.GONE
        binding.assistantOverlaySpeakingIndicator.visibility = if (state == AssistantState.SPEAKING || state == AssistantState.RESPONDING) View.VISIBLE else View.GONE

        binding.assistantOverlayListeningIndicator.animate().cancel()
        binding.assistantOverlaySpeakingIndicator.animate().cancel()
        binding.assistantOverlayVisualCore.animate().cancel()

        when (state) {
            AssistantState.LISTENING -> {
                binding.assistantOverlayListeningIndicator.alpha = 0.35f
                binding.assistantOverlayListeningIndicator.animate()
                    .alpha(1f)
                    .setDuration(550L)
                    .setInterpolator(LinearInterpolator())
                    .withEndAction {
                        if (currentState == AssistantState.LISTENING) {
                            renderVisualState(currentState)
                        }
                    }
                    .start()
                binding.assistantOverlayVisualCore.animate().rotationBy(14f).setDuration(650L).start()
            }

            AssistantState.SPEAKING, AssistantState.RESPONDING -> {
                binding.assistantOverlaySpeakingIndicator.alpha = 0.2f
                binding.assistantOverlaySpeakingIndicator.animate()
                    .alpha(1f)
                    .setDuration(320L)
                    .setInterpolator(LinearInterpolator())
                    .withEndAction {
                        if (currentState == AssistantState.SPEAKING || currentState == AssistantState.RESPONDING) {
                            renderVisualState(currentState)
                        }
                    }
                    .start()
                binding.assistantOverlayVisualCore.animate().scaleX(1.05f).scaleY(1.05f).setDuration(280L)
                    .withEndAction {
                        binding.assistantOverlayVisualCore.animate().scaleX(1f).scaleY(1f).setDuration(280L).start()
                    }
                    .start()
            }

            AssistantState.MUTED, AssistantState.ERROR, AssistantState.SHUTTING_DOWN -> {
                binding.assistantOverlayVisualCore.alpha = 0.74f
            }

            else -> {
                binding.assistantOverlayVisualCore.alpha = 0.96f
                if (state == AssistantState.IDLE || state == AssistantState.WAKE || state == AssistantState.THINKING) {
                    startIdleVisualLoop()
                }
            }
        }
        updateVideoForState(state)
    }

    private fun startIdleVisualLoop() {
        if (!isVisible) return
        if (!(currentState == AssistantState.IDLE || currentState == AssistantState.WAKE || currentState == AssistantState.THINKING)) {
            return
        }
        binding.assistantOverlayVisualCore.animate().cancel()
        binding.assistantOverlayVisualCore.animate()
            .rotationBy(8f)
            .setDuration(4000L)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                startIdleVisualLoop()
            }
            .start()
    }

    private fun stopIdleVisualLoop() {
        binding.assistantOverlayVisualCore.animate().cancel()
    }

    private fun configureVideoLoop() {
        val context = binding.root.context
        binding.assistantOverlayVisualVideo.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f)
        }
        binding.assistantOverlayVisualVideo.setOnErrorListener { _, _, _ ->
            binding.assistantOverlayVisualVideo.visibility = View.GONE
            true
        }
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.nerf_assistant_intro}")
        binding.assistantOverlayVisualVideo.setVideoURI(uri)
    }

    private fun updateVideoForState(state: AssistantState) {
        val shouldShowVideo = state == AssistantState.WAKE ||
            state == AssistantState.LISTENING ||
            state == AssistantState.SPEAKING
        if (!shouldShowVideo) {
            binding.assistantOverlayVisualVideo.pause()
            binding.assistantOverlayVisualVideo.visibility = View.GONE
            return
        }
        binding.assistantOverlayVisualVideo.visibility = View.VISIBLE
        if (!binding.assistantOverlayVisualVideo.isPlaying) {
            binding.assistantOverlayVisualVideo.start()
        }
    }

    private fun setButtonStyle(button: MaterialButton, textColor: Int) {
        button.setTextColor(textColor)
        button.iconTint = ColorStateList.valueOf(textColor)
    }

    private fun tintBackground(view: View, tintColor: Int) {
        val background = view.background ?: return
        val wrapped = DrawableCompat.wrap(background.mutate())
        DrawableCompat.setTint(wrapped, tintColor)
        view.background = wrapped
    }

    private fun assistantErrorColor(): Int {
        val theme = currentTheme()
        return theme.assistantErrorColor
    }

    private fun currentTheme(): NerfTheme {
        activeTheme?.let { return it }
        val config = ConfigRepository.get().config.value
        if (config != null) {
            val resolved = ThemeManager.resolveConfigTheme(binding.root.context, config)
            lastThemeKey = ThemeManager.themeKey(config)
            activeTheme = resolved
            return resolved
        }
        return ThemeManager.resolveActiveTheme(binding.root.context)
    }
}
