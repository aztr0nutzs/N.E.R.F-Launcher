package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityTaskbarSettingsBinding
import com.nerf.launcher.util.TaskbarSettings
import com.nerf.launcher.util.ConfigRepository

class TaskbarSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskbarSettingsBinding
    private var isBindingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskbarSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.taskbar_settings_title)

        setupControls()
        observeConfig()
    }

    private fun setupControls() {
        binding.taskbarEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingState) return@setOnCheckedChangeListener
            updateTaskbarSettings { copy(enabled = isChecked) }
        }

        binding.taskbarHeightSeekbar.max = MAX_HEIGHT_DP - MIN_HEIGHT_DP
        binding.taskbarHeightSeekbar.setOnSeekBarChangeListener(SimpleSeekBarListener { progress, fromUser ->
            if (!fromUser || isBindingState) return@SimpleSeekBarListener
            updateTaskbarSettings { copy(height = progress + MIN_HEIGHT_DP) }
        })

        binding.taskbarIconSizeSeekbar.max = MAX_ICON_SIZE_DP - MIN_ICON_SIZE_DP
        binding.taskbarIconSizeSeekbar.setOnSeekBarChangeListener(SimpleSeekBarListener { progress, fromUser ->
            if (!fromUser || isBindingState) return@SimpleSeekBarListener
            updateTaskbarSettings { copy(iconSize = progress + MIN_ICON_SIZE_DP) }
        })

        binding.taskbarTransparencySeekbar.max = 100
        binding.taskbarTransparencySeekbar.setOnSeekBarChangeListener(SimpleSeekBarListener { progress, fromUser ->
            if (!fromUser || isBindingState) return@SimpleSeekBarListener
            updateTaskbarSettings { copy(transparency = progress / 100f) }
        })

        val backgroundStyleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TASKBAR_BACKGROUND_OPTIONS.map { getString(it.labelRes) }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.taskbarBackgroundStyleSpinner.adapter = backgroundStyleAdapter
        binding.taskbarBackgroundStyleSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (isBindingState) return
                    val selected = TASKBAR_BACKGROUND_OPTIONS[position]
                    updateTaskbarSettings { copy(backgroundStyle = selected.styleRes) }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }

        binding.clearPinnedAppsButton.setOnClickListener { TaskbarController.clearPinnedApps() }
    }

    private fun observeConfig() {
        ConfigRepository.get().config.observe(this) { config ->
            bindTaskbarSettings(config.taskbarSettings)
        }
    }

    private fun bindTaskbarSettings(settings: TaskbarSettings) {
        isBindingState = true

        binding.taskbarEnabledSwitch.isChecked = settings.enabled

        val heightDp = settings.height.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
        binding.taskbarHeightSeekbar.progress = heightDp - MIN_HEIGHT_DP
        binding.taskbarHeightValue.text = getString(R.string.taskbar_settings_height_value, heightDp)

        val iconSizeDp = settings.iconSize.coerceIn(MIN_ICON_SIZE_DP, MAX_ICON_SIZE_DP)
        binding.taskbarIconSizeSeekbar.progress = iconSizeDp - MIN_ICON_SIZE_DP
        binding.taskbarIconSizeValue.text = getString(R.string.taskbar_settings_icon_size_value, iconSizeDp)

        val transparencyPercent = (settings.transparency.coerceIn(0f, 1f) * 100).toInt()
        binding.taskbarTransparencySeekbar.progress = transparencyPercent
        binding.taskbarTransparencyValue.text = getString(
            R.string.taskbar_settings_transparency_value,
            transparencyPercent
        )
        val selectedBackgroundIndex = TASKBAR_BACKGROUND_OPTIONS
            .indexOfFirst { it.styleRes == settings.backgroundStyle }
            .takeIf { it >= 0 }
            ?: 0
        binding.taskbarBackgroundStyleSpinner.setSelection(selectedBackgroundIndex, false)

        val pinnedCount = settings.pinnedApps.size
        binding.pinnedAppsSummary.text = getString(R.string.taskbar_settings_pinned_summary, pinnedCount)
        binding.clearPinnedAppsButton.isEnabled = pinnedCount > 0

        isBindingState = false
    }

    private fun updateTaskbarSettings(transform: TaskbarSettings.() -> TaskbarSettings) {
        TaskbarController.updateSettings(transform)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private class SimpleSeekBarListener(
        private val onProgressChanged: (progress: Int, fromUser: Boolean) -> Unit
    ) : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: android.widget.SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            onProgressChanged(progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit

        override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
    }

    companion object {
        private const val MIN_HEIGHT_DP = 40
        private const val MAX_HEIGHT_DP = 96
        private const val MIN_ICON_SIZE_DP = 24
        private const val MAX_ICON_SIZE_DP = 72
        private val BACKGROUND_LABELS = mapOf(
            android.R.color.background_dark to R.string.taskbar_settings_background_dark,
            android.R.color.background_light to R.string.taskbar_settings_background_light,
            android.R.color.transparent to R.string.taskbar_settings_background_transparent
        )
        private val TASKBAR_BACKGROUND_OPTIONS = listOf(
            android.R.color.background_dark,
            android.R.color.background_light,
            android.R.color.transparent
        )
            .filter { it in TaskbarSettings.supportedBackgroundStyles }
            .map { styleRes ->
                TaskbarBackgroundOption(
                    styleRes = styleRes,
                    labelRes = BACKGROUND_LABELS.getValue(styleRes)
                )
            }

        fun createIntent(context: Context): Intent = Intent(context, TaskbarSettingsActivity::class.java)
    }

    private data class TaskbarBackgroundOption(
        val styleRes: Int,
        val labelRes: Int
    )
}
