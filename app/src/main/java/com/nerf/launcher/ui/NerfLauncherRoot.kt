package com.nerf.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nerf.launcher.state.LauncherViewModel
import com.nerf.launcher.theme.LauncherTheme

/**
 * Top-level Compose entry point wired from [MainActivity.setContent].
 *
 * Injects the config-driven [LauncherTheme] palette from [LauncherViewModel.launcherColors]
 * before delegating to [LauncherAppRoot]. When [ConfigRepository] emits a new [AppConfig]
 * (e.g. the user switches the theme in Settings), [LauncherViewModel.launcherColors]
 * recomposes and the entire Compose tree re-renders with the new palette.
 *
 * Routing logic lives in [LauncherAppRoot]; screen logic lives in the screens.
 */
@Composable
fun NerfLauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    LauncherTheme(colors = viewModel.launcherColors) {
        LauncherAppRoot(
            launcherViewModel = viewModel,
            modifier = modifier.fillMaxSize()
        )
    }
}
