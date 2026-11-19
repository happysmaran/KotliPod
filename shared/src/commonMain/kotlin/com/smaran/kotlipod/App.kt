package com.smaran.kotlipod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.smaran.kotlipod.core.PlatformServices
import com.smaran.kotlipod.core.Pod
import com.smaran.kotlipod.ui.PodView

@Composable
@Preview
fun App(services: PlatformServices = PlatformServices.Unavailable) {
    val pod = remember(services) { Pod(services) }
    var screen by remember(pod) { mutableStateOf(pod.screen) }
    remember(pod) { pod.onChanged = { screen = pod.screen } }

    PodView(screen = screen, onInput = { input -> pod.handle(input) })
}
