package com.smaran.kotlipod.core

// Menus form a tree are walked with an explicit stack so that going back restores
// the selection and scroll position of the screen being returned to.
class Navigator(root: MenuNode, private val visibleRows: Int = DEFAULT_VISIBLE_ROWS) {
    private class Frame(val node: MenuNode, val items: List<MenuNode>) {
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

    private fun scroll(detents: Int) {
        val frame = stack.last()
        if (frame.items.isEmpty()) return
        frame.selected = (frame.selected + detents).coerceIn(0, frame.items.lastIndex)
        frame.firstVisible = when {
            frame.selected < frame.firstVisible -> frame.selected
            frame.selected >= frame.firstVisible + visibleRows -> frame.selected - visibleRows + 1
            else -> frame.firstVisible
        }
    }

    private fun open() {
        val frame = stack.last()
        val child = frame.items.getOrNull(frame.selected) ?: return
        if (child.hasChildren) {
            stack.addLast(frameFor(child))
        }
    }

    private fun back() {
        if (stack.size > 1) {
            stack.removeLast()
        }
    }

    private fun frameFor(node: MenuNode) = Frame(node, node.children())

    companion object {
        const val DEFAULT_VISIBLE_ROWS = 6
    }
}