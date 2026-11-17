package com.smaran.kotlipod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.smaran.kotlipod.core.MenuTree
import com.smaran.kotlipod.core.Navigator
import com.smaran.kotlipod.ui.PodView

@Composable
@Preview
fun App() {
    val navigator = remember { Navigator(MenuTree.build(emptyList())) }
    var screen by remember { mutableStateOf(navigator.screen) }

    PodView(
        screen = screen,
        onInput = { input ->
            navigator.handle(input)
            screen = navigator.screen
        },
    )
}
