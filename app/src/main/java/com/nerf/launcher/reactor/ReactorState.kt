// ReactorState.kt
package com.nerf.launcher.reactor

enum class ReactorMode {
    Idle,
    Active,
    Alert,
    Overdrive
}

enum class ReactorZone {
    Top,
    Right,
    Bottom,
    Left,
    Center
}

data class ReactorConfig(
    val baseSizeDp: Float = 260f,
    val idleOuterRotationSpeed: Float = 8f,   // deg / second (used to derive duration)
    val idleMidRotationSpeed: Float = -16f,
    val idleCoreRotationSpeed: Float = 32f,
    val overdriveMultiplier: Float = 3.2f
)