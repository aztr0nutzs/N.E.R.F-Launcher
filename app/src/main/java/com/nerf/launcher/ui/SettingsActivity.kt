package com.nerf.launcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivitySettingsBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.SettingChange
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: SettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_surface_title)

        binding.openTaskbarSettingsButton.setOnClickListener {
            startActivity(TaskbarSettingsActivity.createIntent(this))
        }

        // Build settings list
        val settings = buildSettingsList()
        adapter = SettingsAdapter(settings) { settingChange ->
            handleSettingChange(settingChange)
        }
        ConfigRepository.get().config.value?.let(adapter::updateFromConfig)
        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
            setHasFixedSize(true)
        }

        ConfigRepository.get().config.observe(this) { config ->
            val theme = ThemeManager.resolveActiveTheme(
                context = this,
                themeName = config.themeName,
                glowIntensity = config.glowIntensity
            )
            ThemeManager.applyWindowTheme(this, theme)
            ThemeManager.applyLauncherShellTheme(binding.root, theme)
            binding.settingsHeaderTitle.setTextColor(theme.hudInfoColor)
            binding.settingsHeaderSubtitle.setTextColor(theme.hudWarningColor)
            binding.openTaskbarSettingsButton.setTextColor(theme.hudAccentColor)
            adapter.updateFromConfig(config)
        }
    }

    /**
     * Build the list of setting items to display.
     */
    private fun buildSettingsList(): List<SettingItem> {
        val config = ConfigRepository.get().config.value
        return listOf(
            SettingItem.Theme(
                title = getString(R.string.settings_theme),
                options = ThemeRepository.allThemeNames
            ),
            SettingItem.IconPack(
                title = getString(R.string.settings_icon_pack),
                options = IconPackManager.getAvailablePacks(this)
            ),
            SettingItem.GlowIntensity(
                title = getString(R.string.settings_glow_intensity),
                initialValue = config?.glowIntensity ?: 0f
            ),
            SettingItem.AnimationSpeed(
                title = getString(R.string.settings_animation_speed),
                initialValue = config?.animationSpeedEnabled ?: false
            ),
            SettingItem.GridSize(
                title = getString(R.string.settings_grid_size),
                initialValue = config?.gridSize ?: 4
            )
        )
    }

    /** React to a setting change from the adapter. */
    private fun handleSettingChange(settingChange: SettingChange) {
        val repo = ConfigRepository.get()
        val current = repo.config.value ?: return
        when (settingChange) {
            is SettingChange.Theme -> {
                if (current.themeName != settingChange.themeName) {
                    repo.updateTheme(settingChange.themeName)
                }
            }

            is SettingChange.IconPack -> {
                if (current.iconPack != settingChange.packName) {
                    IconPackManager.setCurrentPack(this, settingChange.packName)
                }
            }

            is SettingChange.GlowIntensity -> {
                if (current.glowIntensity != settingChange.intensity) {
                    repo.updateGlowIntensity(settingChange.intensity)
                }
            }

            is SettingChange.AnimationSpeed -> {
                if (current.animationSpeedEnabled != settingChange.enabled) {
                    repo.updateAnimationSpeed(settingChange.enabled)
                }
            }

            is SettingChange.GridSize -> {
                if (current.gridSize != settingChange.size) {
                    repo.updateGridSize(settingChange.size)
                }
            }
        }
    }

    // Support for up navigation
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
