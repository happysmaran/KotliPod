package com.smaran.kotlipod

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.smaran.kotlipod.audio.iosPlatformServices

fun MainViewController() = ComposeUIViewController {
    val services = remember { iosPlatformServices() }
    DisposableEffect(services) { onDispose { services.audioPlayer.close() } }
    App(services)
}
