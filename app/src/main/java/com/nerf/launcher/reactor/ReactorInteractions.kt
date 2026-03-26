// ReactorInteractions.kt
package com.nerf.launcher.reactor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

fun Modifier.reactorPointerInput(
    onZoneTap: (ReactorZone) -> Unit,
    onZoneLongPress: (ReactorZone) -> Unit,
    onZoneDoubleTap: (ReactorZone) -> Unit,
    longPressTimeoutMillis: Long
): Modifier = pointerInput(Unit) {
    coroutineScope {
        var lastTapTime = 0L
        var tapJob: Job? = null

        // Drag gestures are used for long‑press and angular swipes
        detectDragGestures(
            onDragStart = { offset ->
                val zone = offset.toReactorZone(size)

                tapJob?.cancel()
                tapJob = launch {
                    delay(longPressTimeoutMillis)
                    // If no tap was completed in this window, treat as long press.
                    onZoneLongPress(zone)
                }
            },
            onDragEnd = {
                // Could be used for swipe‑completion behavior in the future.
            },
            onDragCancel = {
                tapJob?.cancel()
            },
            onDrag = { change, dragAmount ->
                // Interpret angular swipe around ring as potential mode cycling hook.
                val center = Offset(size.width / 2f, size.height / 2f)
                val zone = change.position.toReactorZone(size)
                val isRing = zone != ReactorZone.Center
                if (isRing) {
                    val angleDelta = dragAmount.angularDelta(center)
                    if (angleDelta > 10f) {
                        // Clockwise – hook “next mode” here if desired.
                    } else if (angleDelta < -10f) {
                        // Counter‑clockwise – hook “previous mode” here if desired.
                    }
                }
            }
        )

        // Separate tap / double‑tap detection loop
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val first = event.changes.firstOrNull() ?: continue
                if (!first.changedToUp()) continue

                tapJob?.cancel()
                val zone = first.position.toReactorZone(size)

                val now = System.currentTimeMillis()
                if (now - lastTapTime < 260L) {
                    // Double‑tap
                    onZoneDoubleTap(zone)
                    lastTapTime = 0L
                } else {
                    // Potential single tap – delay slightly to disambiguate from double‑tap.
                    lastTapTime = now
                    launch {
                        delay(220L)
                        if (System.currentTimeMillis() - lastTapTime >= 220L) {
                            onZoneTap(zone)
                        }
                    }
                }
            }
        }
    }
}

private fun Offset.angularDelta(center: Offset): Float {
    // Treat drag vector as angle change around reactor center.
    val beforeAngle = atan2(center.y - (center.y - y), center.x - (center.x - x))
    val afterAngle = atan2(center.y - y, center.x - x)
    return Math.toDegrees((afterAngle - beforeAngle).toDouble()).toFloat()
}

private fun Offset.toReactorZone(size: Size): ReactorZone {
    val center = Offset(size.width / 2f, size.height / 2f)
    val dx = x - center.x
    val dy = y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    val maxRadius = size.minDimension / 2f

    val centerThreshold = maxRadius * 0.35f
    if (distance <= centerThreshold) return ReactorZone.Center

    val angle = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
    return when {
        angle in 315f..360f || angle in 0f..45f -> ReactorZone.Right
        angle in 45f..135f -> ReactorZone.Bottom
        angle in 135f..225f -> ReactorZone.Left
        else -> ReactorZone.Top
    }
}

private fun PointerInputChange.changedToUp(): Boolean =
    !pressed && previousPressed