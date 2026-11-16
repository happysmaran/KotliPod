package com.smaran.kotlipod.core

// A press on the wheel would be ambiguous until it ends: the same touch can be a button click
// or the start of a scroll. As such, releases are used to count as a click only if nothing moved.
class WheelGesture(private val tracker: WheelTracker = WheelTracker()) {
    private var zone = WheelZone.Outside
    private var scrolled = false

    fun down(x: Double, y: Double, timeMs: Long) {
        zone = WheelGeometry.zoneAt(x, y)
        scrolled = false
        if (zone != WheelZone.Outside && zone != WheelZone.Center) {
            tracker.begin(x, y, timeMs)
        }
    }

    fun move(x: Double, y: Double, timeMs: Long): Int {
        if (zone == WheelZone.Outside || zone == WheelZone.Center) {
            return 0
        }

        val detents = tracker.move(x, y, timeMs)
        if (detents != 0) {
            scrolled = true
        }

        return detents
    }

    fun up(): Input? {
        val pressed = zone
        zone = WheelZone.Outside
        if (scrolled) {
            return null
        }

        return when (pressed) {
            WheelZone.Center -> Input.Select
            WheelZone.Menu -> Input.Back
            WheelZone.PlayPause -> Input.PlayPause
            WheelZone.Next -> Input.Next
            WheelZone.Previous -> Input.Previous
            WheelZone.Outside -> null
        }
    }
}