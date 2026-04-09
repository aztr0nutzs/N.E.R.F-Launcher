package com.nerf.launcher.ui

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerf.launcher.state.LauncherViewModel
import com.nerf.launcher.ui.assistant.AssistantScreen
import com.nerf.launcher.ui.assistant.AssistantViewModel
import com.nerf.launcher.ui.screens.HomeLauncherScreen

// ─────────────────────────────────────────────────────────────────────────────
//  Destination
//
//  Minimal screen routing enum for the Compose portion of the launcher.
//  The launcher has exactly two Compose destinations:
//    HOME      — HomeLauncherScreen (reactor command core)
//    ASSISTANT — AssistantScreen (themed assistant overlay)
//
//  No Jetpack Navigation dependency is required; destination ownership is kept
//  inside this file to stay understandable and explicit.
// ─────────────────────────────────────────────────────────────────────────────

private enum class LauncherDestination {
    HOME,
    ASSISTANT
}

// ─────────────────────────────────────────────────────────────────────────────
//  LauncherAppRoot
//
//  Top-level Compose root owned by MainActivity.setContent.
//  Responsibilities:
//    1. Hold the active destination in local state.
//    2. React to LauncherUiState.assistantRequested to switch to ASSISTANT.
//    3. React to AssistantUiState.isVisible == false to return to HOME.
//    4. Render the correct screen for the active destination.
//
//  Both ViewModels are acquired here and passed down; they are never recreated
//  across destination switches because they live in the Activity's ViewModelStore.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LauncherAppRoot(
    launcherViewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // AssistantViewModel is an AndroidViewModel — acquire via factory.
    val assistantViewModel: AssistantViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(application)
    )

    var destination by remember { mutableStateOf(LauncherDestination.HOME) }

    // ── React to assistant request from HomeLauncherScreen ─────────────────
    val assistantRequested = launcherViewModel.uiState.assistantRequested
    LaunchedEffect(assistantRequested) {
        if (assistantRequested) {
            // Consume the flag before switching destination
            launcherViewModel.clearAssistantRequest()
            assistantViewModel.show()
            destination = LauncherDestination.ASSISTANT
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            LauncherDestination.ASSISTANT -> {
                AssistantScreen(
                    viewModel = assistantViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
