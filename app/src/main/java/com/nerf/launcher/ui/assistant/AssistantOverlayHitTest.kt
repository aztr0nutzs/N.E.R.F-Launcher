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
//  CRITICAL DESIGN:
//  ─ This module owns only ROBOT-BODY hit regions (reactor + hand node).
//    All control-widget regions (input, dock, left stack, toggle) are handled
//    exclusively by clickable modifiers in AssistantControlsOverlayMapped.
//    Keeping them out of the global gesture layer prevents double-dispatch.
//
//  ─ For robot-body hit tests, [classify] uses expanded regions from
//    [AssistantOverlayMap] where appropriate.
// ─────────────────────────────────────────────────────────────────────────────

object AssistantOverlayHitTest {

    /**
     * Compute the displayed image rect (screen pixels) for a backplate of
     * [imageW]×[imageH] rendered into a container of [containerW]×[containerH]
     * using ContentScale.Fit (letterboxed / pillarboxed, centred).
     *
     * **This result must be passed to every overlay layer** so all coordinate
     * mapping uses the same reference rectangle.
     */
    fun computeImageRect(
        containerW: Float,
        containerH: Float,
        imageW: Float = 1177f,
        imageH: Float = 2048f
    ): Rect {
        // ContentScale.Fit: uniform scale so the whole image fits inside the container.
        val scale   = minOf(containerW / imageW, containerH / imageH)
        val scaledW = imageW * scale
        val scaledH = imageH * scale
        // Centre the image in the available space (letterbox / pillarbox).
        val offsetX = (containerW - scaledW) / 2f
        val offsetY = (containerH - scaledH) / 2f

        return Rect(
            left   = offsetX,
            top    = offsetY,
            right  = offsetX + scaledW,
            bottom = offsetY + scaledH
        )
    }

    /**
     * Classify a tap at [tapOffset] into a robot-body [HitRegion], or null.
     *
     * Only classifies reactor core/ring and hand-node zones. All other
     * interactive regions (input row, dock, left stack) are handled by the
     * controls overlay's own clickable modifiers and must NOT be tested here.
     *
     * Priority:
     *   1. Reactor core (smallest target — checked first)
     *   2. Reactor ring sectors
     *   3. Hand node
     */
    fun classify(tapOffset: Offset, imageRect: Rect): HitRegion? {
        val map = AssistantOverlayMap

        // 1 + 2. Reactor (physics model handles core vs. sector internally)
        map.reactorPhysics.classify(tapOffset, imageRect)?.let { zone ->
            return when (zone) {
                is ReactorZone.Core   -> HitRegion.ReactorCore
                is ReactorZone.Sector -> HitRegion.ReactorSector(zone.sector)
            }
        }

        // 3. Hand node
        if (map.handNode.containsScreen(tapOffset, imageRect)) return HitRegion.HandNode

        return null
    }

    /** True if [tapOffset] lands within the visor zone. */
    fun isVisorTap(tapOffset: Offset, imageRect: Rect): Boolean =
        AssistantOverlayMap.visorZone.containsScreen(tapOffset, imageRect)

    /** True if [tapOffset] lands on the NERF logo top-right badge. */
    fun isLogoBadgeTap(tapOffset: Offset, imageRect: Rect): Boolean =
        AssistantOverlayMap.logoTopRight.containsScreen(tapOffset, imageRect)
}

// ─────────────────────────────────────────────────────────────────────────────
//  HitRegion — sealed hierarchy of robot-body tappable regions.
//  Control-widget regions (input, dock, left stack) are NOT represented here;
//  they are handled at the controls overlay layer.
// ─────────────────────────────────────────────────────────────────────────────

sealed interface HitRegion {
    data object ReactorCore : HitRegion
    data class  ReactorSector(val sector: com.nerf.launcher.ui.assistant.ReactorSector) : HitRegion
    data object HandNode : HitRegion
}
