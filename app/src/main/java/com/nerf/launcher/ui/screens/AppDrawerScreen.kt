package com.nerf.launcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.theme.LauncherTheme
import com.nerf.launcher.util.AppUtils

// ─────────────────────────────────────────────────────────────────────────────
//  AppDrawerScreen
//
//  Browsable installed-app surface. Reachable from HomeLauncherScreen via the
//  "APPS" utility pill in the top bar. Loads apps using AppUtils.loadInstalledApps()
//  on the IO dispatcher. Renders apps as a grid of N.E.R.F.-styled cells with
//  a searchable header. Tapping any app cell launches it via AppUtils.launchApp().
//
//  Navigation: onNavigateBack() returns to HomeLauncherScreen.
// ─────────────────────────────────────────────────────────────────────────────

private const val GRID_COLUMNS = 4

@Composable
fun AppDrawerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors  = LauncherTheme.colors
    val tokens  = LauncherTheme.tokens

    // ── State ─────────────────────────────────────────────────────────────
    var allApps  by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }
    var query    by rememberSaveable { mutableStateOf("") }

    // Load on first composition — pure IO work, never blocks main thread.
    LaunchedEffect(Unit) {
        allApps  = AppUtils.loadInstalledApps(context)
        isLoaded = true
    }

    // ── Derived filtered list ─────────────────────────────────────────────
    val filteredApps = remember(allApps, query) {
        if (query.isBlank()) allApps
        else {
            val q = query.lowercase()
            allApps.filter {
                it.normalizedAppName.contains(q) || it.normalizedPackageName.contains(q)
            }
        }
    }

    val accent  = colors.accentCyan
    val surface = colors.panelOuter
    val bg      = colors.backgroundTop
    val frame   = colors.frameLine

    // ── Root ─────────────────────────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
    ) {

        // ── Header ──────────────────────────────────────────────────────
        DrawerHeader(
            appCount    = filteredApps.size,
            isLoaded    = isLoaded,
            query       = query,
            onQuery     = { query = it },
            onBack      = onNavigateBack,
            accent      = accent,
            surface     = surface,
            frame       = frame
        )

        // ── Grid ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isLoaded,
            enter   = fadeIn(tween(280)) + slideInVertically(
                animationSpec = tween(320),
                initialOffsetY = { it / 6 }
            )
        ) {
            if (filteredApps.isEmpty() && query.isNotBlank()) {
                DrawerEmptyState(query = query, accent = accent)
            } else {
                LazyVerticalGrid(
                    columns              = GridCells.Fixed(GRID_COLUMNS),
                    contentPadding       = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement  = Arrangement.spacedBy(8.dp),
                    modifier             = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredApps,
                        key   = { "${it.packageName}/${it.className}" }
                    ) { app ->
                        AppCell(
                            app     = app,
                            accent  = accent,
                            surface = surface,
                            frame   = frame,
                            onClick = { AppUtils.launchApp(context, app) }
                        )
                    }
                }
            }
        }

        // ── Loading state (before first emit) ────────────────────────────
        if (!isLoaded) {
            DrawerLoadingState(accent = accent)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerHeader(
    appCount: Int,
    isLoaded: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    accent: androidx.compose.ui.graphics.Color,
    surface: androidx.compose.ui.graphics.Color,
    frame: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(surface, surface.copy(alpha = 0.75f))
                )
            )
            .border(
                width = 0.5.dp,
                color = frame.copy(alpha = 0.40f),
                shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // ── Title row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CutCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .border(0.5.dp, accent.copy(alpha = 0.35f), CutCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick    = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "←",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = accent
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text          = "APP MATRIX",
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 13.sp,
                    letterSpacing = 1.8.sp,
                    color         = LauncherTheme.colors.textPrimary
                )
                Text(
                    text          = "LAUNCH MATRIX  ·  N.E.R.F. SHELL",
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Normal,
                    fontSize      = 8.sp,
                    letterSpacing = 1.2.sp,
                    color         = LauncherTheme.colors.textSecondary.copy(alpha = 0.70f)
                )
            }

            // App count badge
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .border(0.5.dp, accent.copy(alpha = 0.30f), CutCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text       = if (isLoaded) "$appCount APPS" else "· · ·",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 8.sp,
                    color      = accent
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Search field ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(6.dp))
                .background(LauncherTheme.colors.panelInset.copy(alpha = 0.85f))
                .border(0.5.dp, accent.copy(alpha = 0.22f), CutCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = "⌕",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp,
                color      = accent.copy(alpha = 0.60f)
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text       = "Search launch matrix",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize   = 9.sp,
                        color      = LauncherTheme.colors.textSecondary
                    )
                }
                BasicTextField(
                    value          = query,
                    onValueChange  = onQuery,
                    singleLine     = true,
                    cursorBrush    = SolidColor(accent),
                    textStyle      = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize   = 9.sp,
                        color      = LauncherTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onQuery("") }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text       = "✕",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 9.sp,
                        color      = accent.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCell(
    app: AppInfo,
    accent: androidx.compose.ui.graphics.Color,
    surface: androidx.compose.ui.graphics.Color,
    frame: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val pressed = remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(
        targetValue   = if (pressed.value) 0.94f else 1.0f,
        animationSpec = tween(80),
        label         = "cell_scale_${app.packageName}"
    )

    // Abbreviated glyph from first two chars of app name (HUD aesthetic)
    val glyph = app.appName.take(2).uppercase().trimEnd()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(scale)
            .clip(CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp))
            .background(surface)
            .border(
                0.5.dp,
                frame.copy(alpha = 0.22f),
                CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onClick
            )
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Icon placeholder — HUD glyph tile
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CutCornerShape(8.dp))
                .background(accent.copy(alpha = 0.08f))
                .border(0.5.dp, accent.copy(alpha = 0.30f), CutCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = glyph,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp,
                color      = accent.copy(alpha = 0.85f),
                textAlign  = TextAlign.Center
            )
        }

        // App name label
        Text(
            text       = app.appName,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize   = 7.5.sp,
            color      = LauncherTheme.colors.textPrimary.copy(alpha = 0.88f),
            textAlign  = TextAlign.Center,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            lineHeight = 10.sp
        )
    }
}

@Composable
private fun DrawerEmptyState(
    query: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text          = "NO MATCH",
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                fontSize      = 11.sp,
                letterSpacing = 2.sp,
                color         = accent.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = "\"$query\" returned 0 launch targets",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize   = 8.sp,
                color      = LauncherTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun DrawerLoadingState(
    accent: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text          = "SCANNING MATRIX",
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                fontSize      = 11.sp,
                letterSpacing = 2.sp,
                color         = accent.copy(alpha = 0.50f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = "Querying install manifest…",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize   = 8.sp,
                color      = LauncherTheme.colors.textSecondary
            )
        }
    }
}
