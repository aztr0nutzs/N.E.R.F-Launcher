package com.nerf.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nerf.launcher.state.LauncherViewModel
import com.nerf.launcher.ui.screens.HomeLauncherScreen

@Composable
fun NerfLauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    HomeLauncherScreen(
        viewModel = viewModel,
        modifier = modifier.fillMaxSize()
    )
}
