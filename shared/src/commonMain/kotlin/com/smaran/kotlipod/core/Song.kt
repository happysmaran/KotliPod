package com.smaran.kotlipod.core

data class Song(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSeconds: Int,
    val path: String,
)
