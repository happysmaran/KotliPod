package com.smaran.kotlipod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.smaran.kotlipod.audio.androidPlatformServices
import com.smaran.kotlipod.core.PlatformServices

class MainActivity : ComponentActivity() {
    private var services: PlatformServices? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val services = androidPlatformServices(this).also { this.services = it }
        setContent {
            App(services)
        }
    }

    override fun onDestroy() {
        services?.audioPlayer?.close()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}