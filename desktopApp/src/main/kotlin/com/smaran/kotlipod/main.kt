package com.smaran.kotlipod

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KotliPod",
        state = rememberWindowState(size = DpSize(270.dp, 600.dp)),
    ) {
        App()
    }
}