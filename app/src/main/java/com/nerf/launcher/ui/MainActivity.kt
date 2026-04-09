package com.nerf.launcher.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.state.LauncherViewModel

/**
 * Launcher entry point. Acts exclusively as a Compose host.
 *
 * All UI logic lives in the Compose tree rooted at [NerfLauncherRoot].
 * No ViewBinding infrastructure is initialized here; the legacy ViewBinding
 * launcher path was removed because composeUiActive was always true and
 * every ViewBinding setup method was unreachable at runtime.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Suppress default back behavior — the launcher should stay alive.
        // Back-navigation within Compose screens is handled by each screen's
        // own dismiss logic (e.g. AssistantScreen closes via its top bar).
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NerfLauncherRoot(viewModel = viewModel)
        }
    }
}
