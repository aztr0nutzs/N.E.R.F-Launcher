package com.nerf.launcher.util

enum class TaskbarBackgroundStyle(val persistedValue: Int) {
    DARK(0),
    LIGHT(1),
    TRANSPARENT(2);

    companion object {
        val default: TaskbarBackgroundStyle = DARK
        val supportedStyles: Set<TaskbarBackgroundStyle> = entries.toSet()

        fun fromPersistedValue(value: Int): TaskbarBackgroundStyle {
            return when (value) {
                DARK.persistedValue,
                android.R.color.background_dark -> DARK

                LIGHT.persistedValue,
                android.R.color.background_light -> LIGHT

                TRANSPARENT.persistedValue,
                android.R.color.transparent -> TRANSPARENT

                else -> default
            }
        }
    }
}
