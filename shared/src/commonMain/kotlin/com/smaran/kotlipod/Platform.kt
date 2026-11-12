package com.smaran.kotlipod

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform