// AudioReactive.kt
package com.nerf.launcher.ui.reactor.compose.premium

import android.media.audiofx.Visualizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberAudioEnergy(enabled: Boolean): Float {
    // Context kept only for future extension (e.g., permission checks, logging)
    val context = LocalContext.current
    var energy by remember { mutableStateOf(0f) }

    DisposableEffect(enabled) {
        if (!enabled) {
            energy = 0f
            return@DisposableEffect onDispose {}
        }

        // Attach to global output mix (session 0).
        // NOTE: On some devices this may require RECORD_AUDIO permission
        // even though we're not recording, just visualizing.
        val visualizer = try {
            Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform == null || waveform.isEmpty()) return
                            // Root‑mean‑square amplitude normalized to [0,1]
                            val rms = kotlin.math.sqrt(
                                waveform
                                    .map { it.toInt() * it.toInt() }
                                    .average()
                                    .toFloat()
                            )
                            energy = (rms / 128f).coerceIn(0f, 1f)
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) = Unit
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    false
                )
                enabled = true
            }
        } catch (_: Throwable) {
            // Visualizer failed (no permission or not supported) – stay at 0 energy.
            null
        }

        onDispose {
            visualizer?.let {
                try {
                    it.enabled = false
                    it.release()
                } catch (_: Throwable) {
                }
            }
            energy = 0f
        }
    }

    return energy
}
