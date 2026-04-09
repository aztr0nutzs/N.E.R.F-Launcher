package com.nerf.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nerf.launcher.state.LauncherViewModel

/**
 * Top-level Compose entry point wired from [MainActivity.setContent].
 *
 * Delegates to [LauncherAppRoot] which owns the HOME ↔ ASSISTANT destination
 * router and holds both ViewModels. This file intentionally stays thin —
 * routing logic lives in LauncherAppRoot, screen logic lives in the screens.
 */
@Composable
fun NerfLauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    LauncherAppRoot(
        launcherViewModel = viewModel,
        modifier = modifier.fillMaxSize()
    )
}
