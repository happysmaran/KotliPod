package com.smaran.kotlipod.core

// MenuNode Constructuor because many children are produced on demand as most 
// submenus (the albums/artists/playlists) have to be derived and may not be 
// used by the user at all in a run.
class MenuNode private constructor(
    val title: String,
    private val childrenProvider: (() -> List<MenuNode>)?,
) {
    val hasChildren: Boolean get() = childrenProvider != null

    fun children(): List<MenuNode> = childrenProvider?.invoke().orEmpty()

    companion object {
        fun leaf(title: String) = MenuNode(title, null)
        fun branch(title: String, children: () -> List<MenuNode>) = MenuNode(title, children)
    }
}