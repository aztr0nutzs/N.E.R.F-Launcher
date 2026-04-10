package com.nerf.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerf.launcher.state.LauncherViewModel
import com.nerf.launcher.ui.assistant.AssistantScreen
import com.nerf.launcher.ui.assistant.AssistantViewModel
import com.nerf.launcher.ui.screens.AppDrawerScreen
import com.nerf.launcher.ui.screens.HomeLauncherScreen

// ─────────────────────────────────────────────────────────────────────────────
//  Destination
//
//  Minimal screen routing enum for the Compose launcher.
//  The launcher has three Compose destinations:
//    HOME       — HomeLauncherScreen (reactor command core)
//    APP_DRAWER — AppDrawerScreen    (browsable installed app surface)
//    ASSISTANT  — AssistantScreen    (themed assistant overlay)
//
//  No Jetpack Navigation dependency is required; destination ownership is kept
//  inside this file to stay explicit and understandable.
// ─────────────────────────────────────────────────────────────────────────────

private enum class LauncherDestination {
    HOME,
    APP_DRAWER,
    ASSISTANT
}

// ─────────────────────────────────────────────────────────────────────────────
//  LauncherAppRoot
//
//  Top-level Compose router owned by NerfLauncherRoot → MainActivity.setContent.
//  Responsibilities:
//    1. Hold the active destination in local state.
//    2. React to LauncherUiState.assistantRequested   → switch to ASSISTANT.
//    3. React to LauncherUiState.appDrawerRequested   → switch to APP_DRAWER.
//    4. React to AssistantUiState.isVisible == false  → return to HOME.
//    5. Render the correct screen for the active destination.
//
//  Both ViewModels live in the Activity's ViewModelStore and are never recreated
//  across destination switches.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LauncherAppRoot(
    launcherViewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val assistantViewModel: AssistantViewModel = viewModel()

    var destination by remember { mutableStateOf(LauncherDestination.HOME) }

    // ── React to assistant request ─────────────────────────────────────────
    val assistantRequested = launcherViewModel.uiState.assistantRequested
    LaunchedEffect(assistantRequested) {
        if (assistantRequested) {
            launcherViewModel.clearAssistantRequest()
            assistantViewModel.show()
            destination = LauncherDestination.ASSISTANT
        }
    }

    // ── React to app drawer request ────────────────────────────────────────
    val appDrawerRequested = launcherViewModel.uiState.appDrawerRequested
    LaunchedEffect(appDrawerRequested) {
        if (appDrawerRequested) {
            launcherViewModel.clearAppDrawerRequest()
            destination = LauncherDestination.APP_DRAWER
        }
    }

    // ── React to assistant screen being dismissed ──────────────────────────
    val isAssistantVisible = assistantViewModel.uiState.isVisible
    LaunchedEffect(isAssistantVisible) {
        if (!isAssistantVisible && destination == LauncherDestination.ASSISTANT) {
            destination = LauncherDestination.HOME
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
        },
        modifier = modifier,
        label = "launcherDestinationTransition"
    ) { activeDestination ->
        when (activeDestination) {
            LauncherDestination.HOME -> {
                HomeLauncherScreen(
                    viewModel = launcherViewModel,
                    modifier  = Modifier.fillMaxSize()
                )
            }

            LauncherDestination.APP_DRAWER -> {
                AppDrawerScreen(
                    onNavigateBack = { destination = LauncherDestination.HOME },
                    modifier       = Modifier.fillMaxSize()
                )
            }

            LauncherDestination.ASSISTANT -> {
                AssistantScreen(
                    viewModel = assistantViewModel,
                    modifier  = Modifier.fillMaxSize()
                )
            }
        }
    }
}
