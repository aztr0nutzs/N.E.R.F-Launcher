package com.nerf.launcher.ui.reactor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Immutable
data class ReactorMotionState(
    val corePulseScale: Float = 1f,
    val glowBreathScale: Float = 1f,
    val outerRotationDegrees: Float = 0f
)

@Composable
fun rememberReactorMotionState(
    motionSpec: ReactorMotionSpec = ReactorMotionSpec()
): ReactorMotionState {
    val transition = rememberInfiniteTransition(label = "reactorMotion")

    val animatedPulse by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = motionSpec.pulseDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactorPulse"
    )
    val animatedGlow by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = motionSpec.breathingDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactorGlow"
    )
    val animatedRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = motionSpec.rotationDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactorRotation"
    )

    val pulse by rememberUpdatedState(if (motionSpec.pulseCore) animatedPulse else 1f)
    val glow by rememberUpdatedState(if (motionSpec.breatheGlow) animatedGlow else 1f)
    val rotation by rememberUpdatedState(if (motionSpec.rotateOuterRing) animatedRotation else 0f)

    return ReactorMotionState(
        corePulseScale = pulse,
        glowBreathScale = glow,
        outerRotationDegrees = rotation
    )
}
