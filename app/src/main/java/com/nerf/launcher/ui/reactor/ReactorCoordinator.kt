package com.nerf.launcher.ui.reactor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityMainBinding
import com.nerf.launcher.ui.SettingsActivity
import com.nerf.launcher.ui.nodehunter.NodeHunterActivity

class ReactorCoordinator(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val themeNames: List<String>,
    private val getCurrentThemeName: () -> String,
    private val onThemeSelected: (String) -> Unit,
    private val onRefreshDiagnostics: () -> Unit,
    private val onOpenAssistantOverlay: () -> Unit
) {

    fun bind() {
        binding.openSettingsTile.setOnClickListener {
            openSettings()
        }
        binding.reloadTile.setOnClickListener {
            refreshDiagnostics()
        }
        binding.reactorCore.onCoreTapped = {
            cycleTheme()
        }
        binding.reactorCore.onSectorTapped = { sector ->
            when (sector) {
                ReactorModuleView.Sector.TOP -> launchNodeHunter()
                ReactorModuleView.Sector.RIGHT -> launchAssistantDiagnostics()
                ReactorModuleView.Sector.BOTTOM -> refreshDiagnostics()
                ReactorModuleView.Sector.LEFT -> openSettings()
            }
        }
        updateStatus(activity.getString(R.string.reactor_home_status_ready))
    }

    fun updateSyncPreview(sync: Int) {
        binding.reactorSyncValue.text = activity.getString(R.string.modules_reactor_sync, sync)
    }

    private fun cycleTheme() {
        if (themeNames.isEmpty()) return
        val currentTheme = getCurrentThemeName()
        val currentIndex = themeNames.indexOf(currentTheme).takeIf { it >= 0 } ?: 0
        val nextTheme = themeNames[(currentIndex + 1) % themeNames.size]
        onThemeSelected(nextTheme)
        updateStatus(activity.getString(R.string.reactor_home_status_theme, nextTheme))
    }

    private fun launchNodeHunter() {
        updateStatus(activity.getString(R.string.reactor_home_status_node_hunter))
        activity.startActivity(Intent(activity, NodeHunterActivity::class.java))
    }

    private fun launchAssistantDiagnostics() {
        updateStatus(activity.getString(R.string.reactor_home_status_assistant))
        onOpenAssistantOverlay()
    }

    private fun refreshDiagnostics() {
        updateStatus(activity.getString(R.string.reactor_home_status_diagnostics))
        onRefreshDiagnostics()
    }

    private fun openSettings() {
        updateStatus(activity.getString(R.string.reactor_home_status_settings))
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }

    private fun updateStatus(status: String) {
        binding.reactorStatusText.text = status
    }
}
