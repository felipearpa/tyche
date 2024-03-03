package com.felipearpa.tyche.core.paging

import org.junit.Test
import kotlin.test.assertEquals

class CursorPageTest {
    @Test
    fun `given a CursorPage when is mapped then a new CursorPage is returned`() {
        val nextToken = "null"
        val items = listOf(1, 2, 3)
        val page = CursorPage(items, nextToken)
        val newPage = page.map { 2 * it }
        val newItems = listOf(2, 4, 6)

        assertEquals(expected = newItems, actual = newPage.items)
        assertEquals(expected = nextToken, actual = newPage.next)
    }
}