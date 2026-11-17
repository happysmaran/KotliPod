package com.smaran.kotlipod.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WheelGestureTest {
    private fun tap(x: Double, y: Double): Input? {
        val gesture = WheelGesture()
        gesture.down(x, y, 0)
        return gesture.up()
    }
    
    // Do touches reach correct buttons?
    @Test
    fun tapsMapToButtons() {
        assertEquals(Input.Select, tap(0.0, 0.0))
        assertEquals(Input.Back, tap(0.0, -0.8))
        assertEquals(Input.PlayPause, tap(0.0, 0.8))
        assertEquals(Input.Next, tap(0.8, 0.0))
        assertEquals(Input.Previous, tap(-0.8, 0.0))
    }

    // Do touches outside do nothing?
    @Test
    fun tapOutsideTheWheelDoesNothing() {
        assertNull(tap(1.5, 0.0))
    }

    // Does scrollwheel manipulation not trigger buttons?
    @Test
    fun draggingScrollsAndSuppressesTheButtonClick() {
        val gesture = WheelGesture()
        gesture.down(0.0, -0.8, 0)
        val angle = -55.0 * PI / 180.0
        val detents = gesture.move(0.8 * cos(angle), 0.8 * sin(angle), 100)
        assertEquals(2, detents)
        assertNull(gesture.up())
    }

    // (touchscreen) Does wobble count as clicks?
    @Test
    fun smallWobbleStillCountsAsAClick() {
        val gesture = WheelGesture()
        gesture.down(0.0, -0.8, 0)
        val angle = -85.0 * PI / 180.0
        assertEquals(0, gesture.move(0.8 * cos(angle), 0.8 * sin(angle), 100))
        assertEquals(Input.Back, gesture.up())
    }

    // Does dragging from the middle of the wheel (a button) do nothing?
    @Test
    fun dragStartingInTheCenterNeverScrolls() {
        val gesture = WheelGesture()
        gesture.down(0.0, 0.0, 0)
        assertEquals(0, gesture.move(0.0, -0.8, 100))
        assertEquals(Input.Select, gesture.up())
    }
}
