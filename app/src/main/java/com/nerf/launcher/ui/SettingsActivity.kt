package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nerf.launcher.state.LauncherColorsMapper
import com.nerf.launcher.theme.IndustrialLauncherColors
import com.nerf.launcher.theme.LauncherTheme
import com.nerf.launcher.ui.screens.SettingsScreen
import com.nerf.launcher.util.ConfigRepository

// ─────────────────────────────────────────────────────────────────────────────
//  SettingsActivity
//
//  Thin Compose host for the settings surface. Collects ConfigRepository.config
//  (StateFlow) directly via collectAsState() — no observeForever bridge needed.
//
//  No ViewBinding. No imperative applySettingsTheme(). No RecyclerView adapter.
// ─────────────────────────────────────────────────────────────────────────────

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // StateFlow → Compose State. collectAsState() is the idiomatic bridge —
            // no observer lifecycle management required.
            val config by ConfigRepository.get().config.collectAsState()

            // Derive LauncherColors from the live config exactly as LauncherViewModel
            // does — single derivation path, no parallel state.
            val launcherColors = runCatching {
                LauncherColorsMapper.fromConfig(this, config)
            }.getOrDefault(IndustrialLauncherColors)

            LauncherTheme(colors = launcherColors) {
                SettingsScreen(
                    config                = config,
                    onOpenTaskbarSettings = {
                        startActivity(TaskbarSettingsActivity.createIntent(this@SettingsActivity))
                    },
                    onNavigateBack        = { finish() }
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java)
    }
}
