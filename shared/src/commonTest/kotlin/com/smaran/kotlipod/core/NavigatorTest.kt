package com.smaran.kotlipod.core

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    private fun letters(prefix: String, count: Int) =
        (1..count).map { MenuNode.leaf("$prefix$it") }

    private fun navigator(visibleRows: Int = 2) = Navigator(
        MenuNode.branch("Root") {
            listOf(
                MenuNode.branch("A") { letters("a", 5) },
                MenuNode.leaf("B"),
                MenuNode.branch("C") { letters("c", 3) },
            )
        },
        visibleRows,
    )

    @Test
    fun scrollingStopsAtBothEnds() {
        val nav = navigator()
        nav.handle(Input.Scroll(10))
        assertEquals(2, nav.screen.selectedIndex)
        nav.handle(Input.Scroll(-10))
        assertEquals(0, nav.screen.selectedIndex)
    }

    @Test
    fun visibleWindowFollowsTheSelection() {
        val nav = navigator()
        nav.handle(Input.Select)
        nav.handle(Input.Scroll(3))
        assertEquals(3, nav.screen.selectedIndex)
        assertEquals(2, nav.screen.firstVisibleIndex)
        nav.handle(Input.Scroll(-3))
        assertEquals(0, nav.screen.firstVisibleIndex)
    }

    @Test
    fun selectingABranchOpensItsChildren() {
        val nav = navigator()
        nav.handle(Input.Select)
        assertEquals("A", nav.screen.title)
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5"), nav.screen.rows.map { it.text })
    }

    @Test
    fun goingBackRestoresSelectionAndScrollPosition() {
        val nav = navigator()
        nav.handle(Input.Scroll(2))
        nav.handle(Input.Select)
        nav.handle(Input.Scroll(2))
        nav.handle(Input.Back)
        assertEquals("Root", nav.screen.title)
        assertEquals(2, nav.screen.selectedIndex)
        assertEquals(1, nav.screen.firstVisibleIndex)
    }

    @Test
    fun selectingALeafDoesNothing() {
        val nav = navigator()
        nav.handle(Input.Scroll(1))
        nav.handle(Input.Select)
        assertEquals("Root", nav.screen.title)
    }

    @Test
    fun backAtTheRootDoesNothing() {
        val nav = navigator()
        nav.handle(Input.Back)
        assertEquals("Root", nav.screen.title)
    }

    @Test
    fun emptyLibraryStillOpensMusicBrowsingScreens() {
        val nav = Navigator(MenuTree.build(emptyList()))
        nav.handle(Input.Select)
        assertEquals("Music", nav.screen.title)
        nav.handle(Input.Scroll(3))
        nav.handle(Input.Select)
        assertEquals("Songs", nav.screen.title)
        assertEquals(0, nav.screen.rows.size)
        nav.handle(Input.Scroll(1))
        assertEquals(0, nav.screen.selectedIndex)
    }
}
