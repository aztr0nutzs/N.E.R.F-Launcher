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
import com.nerf.launcher.ui.screens.TaskbarSettingsScreen
import com.nerf.launcher.util.ConfigRepository

// ─────────────────────────────────────────────────────────────────────────────
//  TaskbarSettingsActivity
//
//  Thin Compose host — identical pattern to SettingsActivity.
//  Collects ConfigRepository.config (StateFlow) via collectAsState() and
//  injects LauncherTheme before delegating to TaskbarSettingsScreen.
//
//  No ViewBinding. No applyTheme(). No imperative tint setters.
// ─────────────────────────────────────────────────────────────────────────────

class TaskbarSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val config by ConfigRepository.get().config.collectAsState()

            val launcherColors = runCatching {
                LauncherColorsMapper.fromConfig(this, config)
            }.getOrDefault(IndustrialLauncherColors)

            LauncherTheme(colors = launcherColors) {
                TaskbarSettingsScreen(
                    settings   = config.taskbarSettings,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, TaskbarSettingsActivity::class.java)
    }
}
