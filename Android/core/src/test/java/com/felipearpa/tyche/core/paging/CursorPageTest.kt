package com.felipearpa.tyche.core.paging

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CursorPageTest {
    @Test
    fun `given a CursorPage and a transform function when is mapped then a mapped CursorPage is returned`() {
        val newCursorPage = cursorPage.map(transformFunc)
        `verify the mapped cursor page`(actualCursorPage = newCursorPage)
    }

    private fun `verify the mapped cursor page`(actualCursorPage: CursorPage<Int>) {
        for (index in 0..<cursorPage.items.size) {
            actualCursorPage.items[index] shouldBe transformFunc(cursorPage.items[index])
        }

        actualCursorPage.next shouldBe NEXT
    }

    companion object {
        const val NEXT = "next"
        val cursorPage = CursorPage(listOf(1, 2, 3), NEXT)
        val transformFunc: (Int) -> Int = { element -> element * 2 }
    }
}