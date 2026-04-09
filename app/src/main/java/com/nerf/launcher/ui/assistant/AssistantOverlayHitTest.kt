package com.nerf.launcher.ui.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantOverlayHitTest
//
//  Pure-function hit-test utilities for the assistant backplate.
//  All logic operates in screen-pixel space given an [imageRect] — the
//  actual on-screen bounds of the rendered backplate image.
//
//  The displayed image rect is computed via [computeImageRect] which
//  respects ContentScale.Fit aspect-ratio preservation.
// ─────────────────────────────────────────────────────────────────────────────

object AssistantOverlayHitTest {

    /**
     * Compute the displayed image rect (screen pixels) for a backplate of
     * [imageW]×[imageH] rendered into a container of [containerW]×[containerH]
     * using ContentScale.Fit (letterboxed / pillarboxed, centred).
     *
     * Use this result as [imageRect] for all hit-test and placement calls.
     */
    fun computeImageRect(
        containerW: Float,
        containerH: Float,
        imageW: Float = 1177f,
        imageH: Float = 2048f
    ): Rect {
        val scaleX = containerW / imageW
        val scaleY = containerH / imageH
        val scale  = minOf(scaleX, scaleY)          // Fit = uniform scale to fit

        val scaledW = imageW * scale
        val scaledH = imageH * scale
        val offsetX = (containerW - scaledW) / 2f   // horiz letterbox / pillarbox
        val offsetY = (containerH - scaledH) / 2f

        return Rect(
            left   = offsetX,
            top    = offsetY,
            right  = offsetX + scaledW,
            bottom = offsetY + scaledH
        )
    }

    /**
     * Classify a tap at [tapOffset] into a [HitRegion], or null if it falls
     * outside all interactive zones.
     *
     * Regions are tested in priority order:
     *   1. Reactor core / sectors (highest priority — small, precise targets)
     *   2. Hand node
     *   3. Bottom dock buttons
     *   4. Left action stack
     *   5. Send button
     *   6. Input area (mic, emoji, text)
     *   7. Toggle module
     *   8. Transcript / chat pane
     */
    fun classify(tapOffset: Offset, imageRect: Rect): HitRegion? {
        val map = AssistantOverlayMap

        // 1. Reactor zones
        map.reactorPhysics.classify(tapOffset, imageRect)?.let { zone ->
            return when (zone) {
                is ReactorZone.Core -> HitRegion.ReactorCore
                is ReactorZone.Sector -> HitRegion.ReactorSector(zone.sector)
            }
        }

        // 2. Hand node
        if (map.handNode.containsScreen(tapOffset, imageRect)) return HitRegion.HandNode

        // 3. Bottom dock
        if (map.dockCenterCore.containsScreen(tapOffset, imageRect)) return HitRegion.DockCenter
        for ((action, region) in map.dockButtons) {
            if (region.containsScreen(tapOffset, imageRect)) return HitRegion.Dock(action)
        }

        // 4. Left action stack
        for ((action, region) in map.leftActionStack) {
            if (region.containsScreen(tapOffset, imageRect)) return HitRegion.LeftAction(action)
        }

        // 5. Send button
        if (map.sendButton.containsScreen(tapOffset, imageRect)) return HitRegion.Send

        // 6. Input accessories
        if (map.inputMic.containsScreen(tapOffset, imageRect))   return HitRegion.InputMic
        if (map.inputEmoji.containsScreen(tapOffset, imageRect)) return HitRegion.InputEmoji
        if (map.inputTextRegion.containsScreen(tapOffset, imageRect)) return HitRegion.InputText

        // 7. Toggle module
        if (map.toggleModule.containsScreen(tapOffset, imageRect)) return HitRegion.ToggleModule

        // 8. Chat pane
        if (map.transcriptRegion.containsScreen(tapOffset, imageRect)) return HitRegion.Transcript
        if (map.panelOuter.containsScreen(tapOffset, imageRect)) return HitRegion.ChatPanel

        return null
    }

    /**
     * True if [tapOffset] lands within the visor zone (for visor-glow activation).
     */
    fun isVisorTap(tapOffset: Offset, imageRect: Rect): Boolean =
        AssistantOverlayMap.visorZone.containsScreen(tapOffset, imageRect)

    /**
     * True if [tapOffset] lands on the NERF logo top-right badge.
     */
    fun isLogoBadgeTap(tapOffset: Offset, imageRect: Rect): Boolean =
        AssistantOverlayMap.logoTopRight.containsScreen(tapOffset, imageRect)
}

// ─────────────────────────────────────────────────────────────────────────────
//  HitRegion — sealed hierarchy of all tappable regions
// ─────────────────────────────────────────────────────────────────────────────

sealed interface HitRegion {
    // Robot
    data object ReactorCore : HitRegion
    data class  ReactorSector(val sector: com.nerf.launcher.ui.assistant.ReactorSector) : HitRegion
    data object HandNode : HitRegion

    // Input
    data object InputText  : HitRegion
    data object InputMic   : HitRegion
    data object InputEmoji : HitRegion
    data object Send       : HitRegion
    data object ToggleModule : HitRegion

    // Chat
    data object Transcript : HitRegion
    data object ChatPanel  : HitRegion

    // Left stack
    data class LeftAction(val action: com.nerf.launcher.ui.assistant.LeftAction) : HitRegion

    // Dock
    data object DockCenter : HitRegion
    data class  Dock(val action: DockAction) : HitRegion
}
