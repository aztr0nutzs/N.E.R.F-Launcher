// ExampleScreen.kt
package com.nerf.launcher.ui.reactor.compose.premium

import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ReactorDemoScreen() {
    val controller = rememberReactorController()
    var log by remember { mutableStateOf("Ready") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04050A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(16.dp))

            ReactorCore(
                controller = controller,
                onZoneTap = { zone ->
                    log = "Tap: $zone"
                    controller.mode = when (zone) {
                        ReactorZone.Top -> ReactorMode.Active
                        ReactorZone.Right -> ReactorMode.Overdrive
                        ReactorZone.Bottom -> ReactorMode.Idle
                        ReactorZone.Left -> ReactorMode.Alert
                        ReactorZone.Center -> if (controller.mode == ReactorMode.Idle)
                            ReactorMode.Active else ReactorMode.Idle
                    }
                },
                onZoneLongPress = { zone ->
                    log = "Long press: $zone (expand panel)"
                    controller.expand()
                },
                onZoneDoubleTap = { zone ->
                    log = "Double tap $zone → overdrive toggle"
                },
                onOverdriveStart = {
                    log = "OVERDRIVE engaged"
                },
                onOverdriveEnd = {
                    log = "Overdrive ended"
                }
            )

            Text(
                text = log,
                color = Color(0xFFB4C8F
