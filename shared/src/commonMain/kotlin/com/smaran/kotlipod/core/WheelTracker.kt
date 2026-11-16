package com.smaran.kotlipod.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

// Menus scroll in whole rows, but a pointer would report continuous positions on a circle.
// As such, the tracker converts rotation into signed detent counts.

// Clockwise is positive and counterclockwise is negative.
class WheelTracker(
    private val degreesPerDetent: Double = 15.0,
    private val acceleration: (degreesPerSecond: Double) -> Double = ::defaultWheelAcceleration,
) {
    private var lastAngle: Double? = null
    private var lastTimeMs = 0L
    private var pendingDegrees = 0.0

    fun begin(x: Double, y: Double, timeMs: Long) {
        pendingDegrees = 0.0
        lastTimeMs = timeMs
        lastAngle = if (isNearCenter(x, y)) null else angleDegrees(x, y)
    }

    fun move(x: Double, y: Double, timeMs: Long): Int {
        if (isNearCenter(x, y)) {
            return 0
        }

        val angle = angleDegrees(x, y)
        val previous = lastAngle
        lastAngle = angle
        val elapsedMs = timeMs - lastTimeMs
        lastTimeMs = timeMs

        if (previous == null) {
            return 0
        }

        val delta = shortestDeltaDegrees(previous, angle)
        val speed = if (elapsedMs > 0) abs(delta) * 1000.0 / elapsedMs else 0.0
        pendingDegrees += delta * acceleration(speed)

        val detents = (pendingDegrees / degreesPerDetent).toInt()
        pendingDegrees -= detents * degreesPerDetent
        return detents
    }

    private fun isNearCenter(x: Double, y: Double) = x * x + y * y < 1e-6
}

// Slow turns map exactly to entires, while fast sweeps skip through list entries.
fun defaultWheelAcceleration(degreesPerSecond: Double): Double = when {
    degreesPerSecond <= 360.0 -> 1.0
    degreesPerSecond >= 1080.0 -> 4.0
    else -> 1.0 + 3.0 * (degreesPerSecond - 360.0) / (1080.0 - 360.0)
}

internal fun angleDegrees(x: Double, y: Double): Double = atan2(y, x) * 180.0 / PI

// Crossing the 180/-180 seam must read as a small step, not a near-full turn.
internal fun shortestDeltaDegrees(from: Double, to: Double): Double {
    var delta = (to - from) % 360.0
    if (delta > 180.0) {
        delta -= 360.0
    }
    if (delta <= -180.0) {
        delta += 360.0
    }

    return delta
}

enum class WheelZone { Center, Menu, PlayPause, Previous, Next, Outside }

// Coordinates are relative to the wheel center and scaled
object WheelGeometry {
    const val CENTER_FRACTION = 0.36

    fun zoneAt(x: Double, y: Double): WheelZone {
        val radius = sqrt(x * x + y * y)
        return when {
            radius > 1.0 -> WheelZone.Outside
            radius <= CENTER_FRACTION -> WheelZone.Center
            abs(y) >= abs(x) -> if (y < 0) WheelZone.Menu else WheelZone.PlayPause
            else -> if (x < 0) WheelZone.Previous else WheelZone.Next
        }
    }
}