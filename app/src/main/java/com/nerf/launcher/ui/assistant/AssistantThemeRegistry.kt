package com.nerf.launcher.ui.assistant

import androidx.annotation.DrawableRes
import com.nerf.launcher.R

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantThemeRegistry
//
//  Defines each assistant theme's artwork resource and accent palette.
//
//  All overlay region positions are sourced from [AssistantOverlayMap]
//  directly by the Compose overlay system. No per-theme hotspot data is
//  needed or stored here.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Immutable descriptor for a single assistant visual theme.
 *
 * @param id Stable identifier for persistence and switching.
 * @param displayName Human-readable label shown in the theme picker strip.
 * @param backplateRes Drawable resource ID for the full-screen artwork backplate.
 * @param palette Theme-specific accent colors for overlays and effects.
 */
data class AssistantThemeConfig(
    val id: AssistantThemeId,
    val displayName: String,
    @DrawableRes val backplateRes: Int,
    val palette: AssistantThemePalette
)

/**
 * Stable enum for the three shipped assistant themes.
 */
enum class AssistantThemeId {
    PHANTOM_BLACK,
    NERF_ORANGE,
    BLUEPRINT_STONE
}

/**
 * Theme-specific accent colors for assistant overlay elements.
 * Stored as ARGB hex longs for direct use with [androidx.compose.ui.graphics.Color].
 */
data class AssistantThemePalette(
    val visorGlow: Long,
    val coreGlow: Long,
    val scanSweep: Long,
    val chatPanelBg: Long,
    val chatPanelBorder: Long,
    val chatUserBubble: Long,
    val chatAssistantBubble: Long,
    val controlAccent: Long,
    val controlSurface: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val errorGlow: Long,
    val dockSurface: Long
)

// ─────────────────────────────────────────────────────────────────────────────
//  Registry
// ─────────────────────────────────────────────────────────────────────────────

object AssistantThemeRegistry {

    private val phantomBlack = AssistantThemeConfig(
        id           = AssistantThemeId.PHANTOM_BLACK,
        displayName  = "PHANTOM BLACK",
        backplateRes = R.drawable.assistant_theme_phantom_black,
        palette      = AssistantThemePalette(
            visorGlow           = 0xFF27E7FF,
            coreGlow            = 0xFF27E7FF,
            scanSweep           = 0xFF27E7FF,
            chatPanelBg         = 0xCC060B12,
            chatPanelBorder     = 0x6627E7FF,
            chatUserBubble      = 0xCC101820,
            chatAssistantBubble = 0xCC0A1420,
            controlAccent       = 0xFF27E7FF,
            controlSurface      = 0xCC0B1117,
            textPrimary         = 0xFFE6FBFF,
            textSecondary       = 0xFF9AB5BC,
            errorGlow           = 0xFFFF6A6A,
            dockSurface         = 0xCC080D14
        )
    )

    private val nerfOrange = AssistantThemeConfig(
        id           = AssistantThemeId.NERF_ORANGE,
        displayName  = "NERF ORANGE",
        backplateRes = R.drawable.assistant_theme_nerf_orange,
        palette      = AssistantThemePalette(
            visorGlow           = 0xFFFF9F43,
            coreGlow            = 0xFFFF7B1C,
            scanSweep           = 0xFFFF9F43,
            chatPanelBg         = 0xCC100A04,
            chatPanelBorder     = 0x66FF9F43,
            chatUserBubble      = 0xCC1A1008,
            chatAssistantBubble = 0xCC140C04,
            controlAccent       = 0xFFFF9F43,
            controlSurface      = 0xCC120C06,
            textPrimary         = 0xFFFFF0E0,
            textSecondary       = 0xFFBCA080,
            errorGlow           = 0xFFFF4444,
            dockSurface         = 0xCC0E0904
        )
    )

    private val blueprintStone = AssistantThemeConfig(
        id           = AssistantThemeId.BLUEPRINT_STONE,
        displayName  = "BLUEPRINT STONE",
        backplateRes = R.drawable.assistant_theme_blueprint_stone,
        palette      = AssistantThemePalette(
            visorGlow           = 0xFF78A8D0,
            coreGlow            = 0xFF5A90C0,
            scanSweep           = 0xFF78A8D0,
            chatPanelBg         = 0xCC080E16,
            chatPanelBorder     = 0x6678A8D0,
            chatUserBubble      = 0xCC0C1624,
            chatAssistantBubble = 0xCC08101C,
            controlAccent       = 0xFF78A8D0,
            controlSurface      = 0xCC0A1018,
            textPrimary         = 0xFFD8E8F4,
            textSecondary       = 0xFF8CA8BC,
            errorGlow           = 0xFFE06050,
            dockSurface         = 0xCC070C14
        )
    )

    private val registry: Map<AssistantThemeId, AssistantThemeConfig> = mapOf(
        AssistantThemeId.PHANTOM_BLACK   to phantomBlack,
        AssistantThemeId.NERF_ORANGE     to nerfOrange,
        AssistantThemeId.BLUEPRINT_STONE to blueprintStone
    )

    val allThemes: List<AssistantThemeConfig> = listOf(phantomBlack, nerfOrange, blueprintStone)

    val defaultTheme: AssistantThemeConfig = phantomBlack

    fun get(id: AssistantThemeId): AssistantThemeConfig = registry[id] ?: defaultTheme

    fun next(current: AssistantThemeId): AssistantThemeConfig {
        val ids = AssistantThemeId.values()
        val nextIndex = (ids.indexOf(current) + 1) % ids.size
        return get(ids[nextIndex])
    }
}
