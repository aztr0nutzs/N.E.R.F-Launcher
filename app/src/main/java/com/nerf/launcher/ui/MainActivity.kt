package com.nerf.launcher.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nerf.launcher.state.LauncherViewModel

/**
 * Launcher entry point — acts exclusively as a Compose host.
 *
 * All UI logic lives in the Compose tree rooted at [NerfLauncherRoot].
 * [LauncherViewModel] is an [AndroidViewModel]; the default
 * [SavedStateViewModelFactory] provided by [AppCompatActivity] handles its
 * construction automatically via [viewModels].
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Suppress default back behavior — the launcher must always stay alive.
        // Back-navigation within Compose screens is handled by each screen's own
        // dismiss logic (e.g. AssistantScreen closes via AssistantEvent.Dismiss).
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NerfLauncherRoot(viewModel = viewModel)
        }
    }
}
