package com.smaran.kotlipod

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.smaran.kotlipod.audio.jvmPlatformServices

fun main() = application {
    val services = remember { jvmPlatformServices() }

    Window(
        onCloseRequest = {
            services.audioPlayer.close()
            exitApplication()
        },
        title = "KotliPod",
        state = rememberWindowState(size = DpSize(270.dp, 600.dp)),
    ) {
        App(services)
    }
}