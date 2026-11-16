package com.smaran.kotlipod.core

// Everything uses this single input vocabulary interface, that way everyone
// knows where is what and how they connect to each other.

// (sealed means no children of interface)
sealed interface Input {
    data class Scroll(val detents: Int) : Input
    data object Select : Input
    data object Back : Input
    data object PlayPause : Input
    data object Next : Input
    data object Previous : Input
}