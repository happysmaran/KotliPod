package com.smaran.kotlipod.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WheelTrackerTest {
    private fun x(degrees: Double) = cos(degrees * PI / 180.0)
    private fun y(degrees: Double) = sin(degrees * PI / 180.0)

    // Sweeps from one angle to another in equal steps and returns the summed detents.
    private fun sweep(
        tracker: WheelTracker,
        from: Double,
        to: Double,
        steps: Int,
        msPerStep: Long,
    ): Int {
        tracker.begin(x(from), y(from), 0)
        var total = 0
        for (i in 1..steps) {
            val angle = from + (to - from) * i / steps
            total += tracker.move(x(angle), y(angle), i * msPerStep)
        }
        return total
    }

    private fun plain() = WheelTracker(acceleration = { 1.0 })

    // Does turning mean correct movement?
    @Test
    fun clockwiseQuarterTurnScrollsForward() {
        assertEquals(6, sweep(plain(), 0.0, 100.0, steps = 20, msPerStep = 100))
    }
    @Test
    fun counterClockwiseQuarterTurnScrollsBackward() {
        assertEquals(-6, sweep(plain(), 100.0, 0.0, steps = 20, msPerStep = 100))
    }

    // Does crossing the circle "seam" result in continued correct movement?
    @Test
    fun crossingTheSeamClockwiseIsASmallStep() {
        assertEquals(1, sweep(plain(), 350.0, 370.0, steps = 4, msPerStep = 100))
    }
    @Test
    fun crossingTheSeamCounterClockwiseIsASmallStep() {
        assertEquals(-1, sweep(plain(), 370.0, 350.0, steps = 4, msPerStep = 100))
    }

    // Does changing rotation correctly change movement?
    @Test
    fun reversingMidStrokeCancelsOutRemainingRotation() {
        val tracker = plain()
        tracker.begin(x(0.0), y(0.0), 0)
        val forward = tracker.move(x(10.0), y(10.0), 100)
        val back = tracker.move(x(0.0), y(0.0), 200)
        assertEquals(0, forward + back)
    }

    // Do different speeds of rotation affect movement speed?
    @Test
    fun fastSweepSkipsMoreRowsThanSlowSweep() {
        val slow = sweep(WheelTracker(), 0.0, 90.0, steps = 90, msPerStep = 100)
        val fast = sweep(WheelTracker(), 0.0, 90.0, steps = 3, msPerStep = 10)
        assertTrue(fast > slow, "fast=$fast slow=$slow")
    }

    @Test
    fun pointerAtTheHubProducesNoScroll() {
        val tracker = plain()
        tracker.begin(x(0.0), y(0.0), 0)
        assertEquals(0, tracker.move(0.0, 0.0, 100))
    }

    @Test
    fun shortestDeltaHandlesWraparound() {
        assertEquals(20.0, shortestDeltaDegrees(350.0, 10.0), 1e-9)
        assertEquals(-20.0, shortestDeltaDegrees(10.0, 350.0), 1e-9)
        assertEquals(180.0, shortestDeltaDegrees(0.0, 180.0), 1e-9)
    }

    @Test
    fun accelerationIsMonotonicAndBounded() {
        var previous = 0.0
        for (speed in 0..1500 step 50) {
            val multiplier = defaultWheelAcceleration(speed.toDouble())
            assertTrue(multiplier >= previous)
            assertTrue(multiplier in 1.0..4.0)
            previous = multiplier
        }
    }
}
