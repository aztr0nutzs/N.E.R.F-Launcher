package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityTaskbarSettingsBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.TaskbarSettings

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

        binding.clearPinnedAppsButton.setOnClickListener {
            TaskbarController.clearPinnedApps(this)
        }
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

        val pinnedCount = settings.pinnedApps.size
        binding.pinnedAppsSummary.text = getString(R.string.taskbar_settings_pinned_summary, pinnedCount)
        binding.clearPinnedAppsButton.isEnabled = pinnedCount > 0

        isBindingState = false
    }

    private fun updateTaskbarSettings(transform: TaskbarSettings.() -> TaskbarSettings) {
        val repo = ConfigRepository.get()
        val current = repo.config.value ?: return
        repo.updateTaskbarSettings(current.taskbarSettings.transform())
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

        fun createIntent(context: Context): Intent = Intent(context, TaskbarSettingsActivity::class.java)
    }
}
