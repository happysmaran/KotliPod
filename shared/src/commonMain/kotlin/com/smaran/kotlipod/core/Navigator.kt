package com.smaran.kotlipod.core

// Menus form a tree walked with an explicit stack so that going back restores
// the selection and scroll position of the screen being returned to.
class Navigator(root: MenuNode, private val visibleRows: Int = DEFAULT_VISIBLE_ROWS) {
    private class Frame(val node: MenuNode, var items: List<MenuNode>) {
        var selected = 0
        var firstVisible = 0
    }

    private val stack = ArrayDeque<Frame>().apply { addLast(frameFor(root)) }

    val screen: ListScreen
        get() {
            val frame = stack.last()
            return ListScreen(
                title = frame.node.title,
                rows = frame.items.map { ListRow(it.title, it.hasChildren) },
                selectedIndex = frame.selected,
                firstVisibleIndex = frame.firstVisible,
                visibleRowCount = visibleRows,
            )
        }

    fun handle(input: Input) {
        when (input) {
            is Input.Scroll -> scroll(input.detents)
            Input.Select -> open()
            Input.Back -> back()
            Input.PlayPause, Input.Next, Input.Previous -> Unit
        }
    }

    // Lists that depend on state outside the menu (the library, the chosen folder) change while
    // their screen is open, so the visible screen can be rebuilt without losing the selection.
    fun refresh() {
        val frame = stack.last()
        frame.items = frame.node.children()
        frame.selected = frame.selected.coerceIn(0, maxOf(0, frame.items.lastIndex))
        fitWindow(frame)
    }

    private fun scroll(detents: Int) {
        val frame = stack.last()
        if (frame.items.isEmpty()) return
        // Lists stop at their ends instead of wrapping, matching the original device.
        frame.selected = (frame.selected + detents).coerceIn(0, frame.items.lastIndex)
        fitWindow(frame)
    }

    private fun fitWindow(frame: Frame) {
        val lastStart = maxOf(0, frame.items.size - visibleRows)
        var start = frame.firstVisible.coerceIn(0, lastStart)
        if (frame.selected < start) start = frame.selected
        if (frame.selected >= start + visibleRows) start = frame.selected - visibleRows + 1
        frame.firstVisible = start
    }

    private fun open() {
        val frame = stack.last()
        val child = frame.items.getOrNull(frame.selected) ?: return
        when {
            child.isAction -> child.select()
            child.hasChildren -> stack.addLast(frameFor(child))
        }
    }

    private fun back() {
        if (stack.size > 1) stack.removeLast()
    }

    private fun frameFor(node: MenuNode) = Frame(node, node.children())

    companion object {
        const val DEFAULT_VISIBLE_ROWS = 6
    }
}
