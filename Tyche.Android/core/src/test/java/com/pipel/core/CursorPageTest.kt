package com.pipel.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CursorPageTest {

    @Test
    fun `given a CursorPage when it's mapped then a new CursorPage is returned`() {
        val nextToken = "null"
        val items = listOf(1, 2, 3)
        val page = CursorPage(items, nextToken)
        val newPage = page.map { 2 * it }
        val newItems = listOf(2, 4, 6)

        assertEquals(newPage.items, newItems)
        assertEquals(nextToken, newPage.nextToken)
    }

}