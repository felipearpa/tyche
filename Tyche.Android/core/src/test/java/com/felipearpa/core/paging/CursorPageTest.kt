package com.felipearpa.core.paging

import org.junit.Assert.assertEquals
import org.junit.Test

class CursorPageTest {

    @Test
    fun `given a CursorPage when is mapped then a new CursorPage is returned`() {
        val nextToken = "null"
        val items = listOf(1, 2, 3)
        val page = CursorPage(items, nextToken)
        val newPage = page.map { 2 * it }
        val newItems = listOf(2, 4, 6)

        assertEquals(newPage.items, newItems)
        assertEquals(nextToken, newPage.next)
    }
}