package com.smaran.kotlipod.core

data class ListRow(val text: String, val hasChildren: Boolean)

// What the renderer needs to draw one frame of a list screen; it carries no navigation logic.
data class ListScreen(
    val title: String,
    val rows: List<ListRow>,
    val selectedIndex: Int,
    val firstVisibleIndex: Int,
    val visibleRowCount: Int,
)
