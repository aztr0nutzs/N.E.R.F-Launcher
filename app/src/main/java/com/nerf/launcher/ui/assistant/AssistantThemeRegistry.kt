package com.nerf.launcher.ui.assistant

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Rect
import com.nerf.launcher.R

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantThemeRegistry
//
//  Defines each assistant theme's artwork resource, accent palette, and
//  normalized overlay hotspot bounds. Bounds are expressed as fractions of
//  the full backplate image (0f–1f on each axis) so they adapt to any screen
//  density or aspect ratio.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Immutable descriptor for a single assistant visual theme.
 *
 * @param id Stable identifier for persistence and switching.
 * @param displayName Human-readable label shown in the theme picker strip.
 * @param backplateRes Drawable resource ID for the full-screen artwork backplate.
 * @param hotspots Region bounds for interactive overlay panes.
 * @param palette Theme-specific accent colors for overlays and effects.
 */
data class AssistantThemeConfig(
    val id: AssistantThemeId,
    val displayName: String,
    @DrawableRes val backplateRes: Int,
    val hotspots: AssistantHotspots,
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
 * Normalized (0f–1f) rectangular bounds for each interactive overlay zone.
 *
 * Each [Rect] describes a fraction of the backplate where:
 * - left/top = fractional offset from the left/top edge
 * - right/bottom = fractional offset from the left/top edge (not width/height)
 *
 * For example, [Rect(0.08f, 0.38f, 0.92f, 0.68f)] means the zone starts at
 * 8% from the left, 38% from the top and extends to 92% width, 68% height.
 */
data class AssistantHotspots(
    /** Chat transcript pane region. */
    val chatPane: Rect,
    /** Bottom dock row for action buttons. */
    val dockRow: Rect,
    /** Microphone / voice input button. */
    val micButton: Rect,
    /** Chest/core reactor zone (tap to trigger effects). */
    val chestCore: Rect,
    /** Hand projection zone (tap for projection overlay). */
    val handProjection: Rect,
    /** Optional side telemetry module (left). */
    val sideModuleLeft: Rect? = null,
    /** Optional side telemetry module (right). */
    val sideModuleRight: Rect? = null
)

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
        id = AssistantThemeId.PHANTOM_BLACK,
        displayName = "PHANTOM BLACK",
        backplateRes = R.drawable.assistant_theme_phantom_black,
        hotspots = AssistantHotspots(
            chatPane = Rect(0.06f, 0.34f, 0.94f, 0.64f),
            dockRow = Rect(0.06f, 0.86f, 0.94f, 0.96f),
            micButton = Rect(0.38f, 0.87f, 0.62f, 0.95f),
            chestCore = Rect(0.30f, 0.18f, 0.70f, 0.34f),
            handProjection = Rect(0.65f, 0.45f, 0.94f, 0.65f),
            sideModuleLeft = Rect(0.02f, 0.20f, 0.16f, 0.50f),
            sideModuleRight = Rect(0.84f, 0.20f, 0.98f, 0.50f)
        ),
        palette = AssistantThemePalette(
            visorGlow = 0xFF27E7FF,
            coreGlow = 0xFF27E7FF,
            scanSweep = 0xFF27E7FF,
            chatPanelBg = 0xCC060B12,
            chatPanelBorder = 0x6627E7FF,
            chatUserBubble = 0xCC101820,
            chatAssistantBubble = 0xCC0A1420,
            controlAccent = 0xFF27E7FF,
            controlSurface = 0xCC0B1117,
            textPrimary = 0xFFE6FBFF,
            textSecondary = 0xFF9AB5BC,
            errorGlow = 0xFFFF6A6A,
            dockSurface = 0xCC080D14
        )
    )

    private val nerfOrange = AssistantThemeConfig(
        id = AssistantThemeId.NERF_ORANGE,
        displayName = "NERF ORANGE",
        backplateRes = R.drawable.assistant_theme_nerf_orange,
        hotspots = AssistantHotspots(
            chatPane = Rect(0.05f, 0.36f, 0.95f, 0.66f),
            dockRow = Rect(0.05f, 0.85f, 0.95f, 0.96f),
            micButton = Rect(0.36f, 0.86f, 0.64f, 0.95f),
            chestCore = Rect(0.28f, 0.16f, 0.72f, 0.36f),
            handProjection = Rect(0.62f, 0.44f, 0.95f, 0.66f),
            sideModuleLeft = Rect(0.01f, 0.18f, 0.14f, 0.48f),
            sideModuleRight = Rect(0.86f, 0.18f, 0.99f, 0.48f)
        ),
        palette = AssistantThemePalette(
            visorGlow = 0xFFFF9F43,
            coreGlow = 0xFFFF7B1C,
            scanSweep = 0xFFFF9F43,
            chatPanelBg = 0xCC100A04,
            chatPanelBorder = 0x66FF9F43,
            chatUserBubble = 0xCC1A1008,
            chatAssistantBubble = 0xCC140C04,
            controlAccent = 0xFFFF9F43,
            controlSurface = 0xCC120C06,
            textPrimary = 0xFFFFF0E0,
            textSecondary = 0xFFBCA080,
            errorGlow = 0xFFFF4444,
            dockSurface = 0xCC0E0904
        )
    )

    private val blueprintStone = AssistantThemeConfig(
        id = AssistantThemeId.BLUEPRINT_STONE,
        displayName = "BLUEPRINT STONE",
        backplateRes = R.drawable.assistant_theme_blueprint_stone,
        hotspots = AssistantHotspots(
            chatPane = Rect(0.07f, 0.35f, 0.93f, 0.65f),
            dockRow = Rect(0.07f, 0.86f, 0.93f, 0.96f),
            micButton = Rect(0.37f, 0.87f, 0.63f, 0.95f),
            chestCore = Rect(0.32f, 0.17f, 0.68f, 0.35f),
            handProjection = Rect(0.64f, 0.46f, 0.93f, 0.64f),
            sideModuleLeft = Rect(0.03f, 0.22f, 0.18f, 0.52f)
        ),
        palette = AssistantThemePalette(
            visorGlow = 0xFF78A8D0,
            coreGlow = 0xFF5A90C0,
            scanSweep = 0xFF78A8D0,
            chatPanelBg = 0xCC080E16,
            chatPanelBorder = 0x6678A8D0,
            chatUserBubble = 0xCC0C1624,
            chatAssistantBubble = 0xCC08101C,
            controlAccent = 0xFF78A8D0,
            controlSurface = 0xCC0A1018,
            textPrimary = 0xFFD8E8F4,
            textSecondary = 0xFF8CA8BC,
            errorGlow = 0xFFE06050,
            dockSurface = 0xCC070C14
        )
    )

    private val registry: Map<AssistantThemeId, AssistantThemeConfig> = mapOf(
        AssistantThemeId.PHANTOM_BLACK to phantomBlack,
        AssistantThemeId.NERF_ORANGE to nerfOrange,
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
