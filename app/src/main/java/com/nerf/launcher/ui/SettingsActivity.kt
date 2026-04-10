package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.nerf.launcher.state.LauncherColorsMapper
import com.nerf.launcher.theme.IndustrialLauncherColors
import com.nerf.launcher.theme.LauncherTheme
import com.nerf.launcher.ui.screens.SettingsScreen
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ConfigRepository

// ─────────────────────────────────────────────────────────────────────────────
//  SettingsActivity
//
//  Thin Compose host for the settings surface. Bridges ConfigRepository.config
//  (LiveData) to Compose State using produceState so any change written through
//  ConfigRepository immediately triggers recomposition.
//
//  No ViewBinding. No imperative applySettingsTheme(). No RecyclerView adapter.
// ─────────────────────────────────────────────────────────────────────────────

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Bridge LiveData → Compose State with no extra dependencies.
            // produceState seeds from the current LiveData value and re-emits
            // on every subsequent update via observeForever.
            val config: AppConfig? by produceState<AppConfig?>(
                initialValue = ConfigRepository.get().config.value
            ) {
                val liveData = ConfigRepository.get().config
                val observer = androidx.lifecycle.Observer<AppConfig> { value = it }
                liveData.observeForever(observer)
                awaitDispose { liveData.removeObserver(observer) }
            }

            // Derive LauncherColors from the live config exactly as LauncherViewModel
            // does — single derivation path, no parallel state.
            val launcherColors = config
                ?.let { LauncherColorsMapper.fromConfig(this, it) }
                ?: IndustrialLauncherColors

            LauncherTheme(colors = launcherColors) {
                config?.let { safeConfig ->
                    SettingsScreen(
                        config                = safeConfig,
                        onOpenTaskbarSettings = {
                            startActivity(TaskbarSettingsActivity.createIntent(this@SettingsActivity))
                        },
                        onNavigateBack        = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java)
    }
}

