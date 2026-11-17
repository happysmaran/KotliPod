package com.smaran.kotlipod.ui

import androidx.compose.ui.graphics.Color

// All geometry is expressed in screen pixels of the emulated 176x132 display, so the
// drawing code never mixes units and the whole pod scales as a single unit.
// Real device: 40 x 90 mm body with a 176x132 screen of 38 mm diagonal (220 px), so one
// unit is 38/220 mm and the body works out to 231.6 x 521 units (ratio 4:9).
internal object PodLayout {
    const val BODY_WIDTH = 231.6f
    const val BODY_HEIGHT = 521f
    const val BODY_CORNER = 23f

    const val SCREEN_LEFT = 27.8f
    const val SCREEN_TOP = 32f
    const val SCREEN_WIDTH = 176f
    const val SCREEN_HEIGHT = 132f

    const val TITLE_BAR_HEIGHT = 20f
    const val ROW_HEIGHT = 18f

    const val WHEEL_CENTER_X = 115.8f
    const val WHEEL_CENTER_Y = 400f
    const val WHEEL_RADIUS = 90f

    // Wheel gestures are interpreted relative to the wheel, independent of the pod's size on screen.
    fun toWheel(unitsX: Float, unitsY: Float): Pair<Double, Double> =
        ((unitsX - WHEEL_CENTER_X) / WHEEL_RADIUS).toDouble() to
            ((unitsY - WHEEL_CENTER_Y) / WHEEL_RADIUS).toDouble()
}

// Common Palette
internal object Palette {
    val Backdrop = Color(0xFF2B2B2E)
    val Body = Color(0xFF171719)
    val BodyEdge = Color(0xFF38383B)
    val Bezel = Color(0xFF080808)
    val ScreenBackground = Color(0xFFFFFFFF)
    val TitleBarTop = Color(0xFFF6F6F6)
    val TitleBarBottom = Color(0xFFCDCDCD)
    val TitleBarLine = Color(0xFF8C8C8C)
    val Selection = Color(0xFF316AC5)
    val Battery = Color(0xFF4CAF50)
    val WheelRing = Color(0xFF353537)
    val WheelButton = Color(0xFF29292B)
    val WheelGlyph = Color(0xFFAAAAAA)
}