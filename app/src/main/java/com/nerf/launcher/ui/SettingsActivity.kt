package com.nerf.launcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivitySettingsBinding
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.SettingsType
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: SettingsAdapter
    private var lastObservedConfig: AppConfig? = null

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
        adapter = SettingsAdapter(settings) { setting ->
            handleSettingChange(setting)
        }
        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
            setHasFixedSize(true)
        }

        ConfigRepository.get().config.observe(this) { config ->
            if (lastObservedConfig == config) return@observe
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
            lastObservedConfig = config
        }
    }

    /**
     * Build the list of setting items to display.
     */
    private fun buildSettingsList(): List<SettingItem> {
        val config = ConfigRepository.get().config.value
        val list = mutableListOf<SettingItem>()
        // Theme selector
        list.add(SettingItem(
            SettingsType.THEME,
            getString(R.string.settings_theme),
            ThemeRepository.allThemeNames.joinToString { it }
        ))
        // Icon pack selector
        list.add(SettingItem(
            SettingsType.ICON_PACK,
            getString(R.string.settings_icon_pack),
            IconPackManager.getAvailablePacks(this).joinToString { it }
        ))
        // Glow intensity slider
        list.add(SettingItem(
            SettingsType.GLOW_INTENSITY,
            getString(R.string.settings_glow_intensity),
            config?.glowIntensity ?: 0f
        ))
        // Animation speed toggle
        list.add(SettingItem(
            SettingsType.ANIMATION_SPEED,
            getString(R.string.settings_animation_speed),
            config?.animationSpeedEnabled ?: false
        ))
        // Grid size selector
        list.add(SettingItem(
            SettingsType.GRID_SIZE,
            getString(R.string.settings_grid_size),
            config?.gridSize ?: 4
        ))
        return list
    }

    /** React to a setting change from the adapter. */
    private fun handleSettingChange(setting: SettingItem) {
        val repo = ConfigRepository.get()
        val current = repo.config.value ?: return
        when (setting.type) {
            SettingsType.THEME -> {
                val themeName = setting.payload as String
                if (current.themeName != themeName) {
                    repo.updateTheme(themeName)
                }
            }
            SettingsType.ICON_PACK -> {
                val packName = setting.payload as String
                if (current.iconPack != packName) {
                    IconPackManager.setCurrentPack(this, packName)
                }
            }
            SettingsType.GLOW_INTENSITY -> {
                val intensity = setting.payload as Float
                if (current.glowIntensity != intensity) {
                    repo.updateGlowIntensity(intensity)
                }
            }
            SettingsType.ANIMATION_SPEED -> {
                val enabled = setting.payload as Boolean
                if (current.animationSpeedEnabled != enabled) {
                    repo.updateAnimationSpeed(enabled)
                }
            }
            SettingsType.GRID_SIZE -> {
                val size = setting.payload as Int
                if (current.gridSize != size) {
                    repo.updateGridSize(size)
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
