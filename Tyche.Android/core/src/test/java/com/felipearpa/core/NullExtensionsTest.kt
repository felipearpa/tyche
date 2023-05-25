package com.felipearpa.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NullExtensionsTest {

    @Test
    fun `given a null variable when ifNull is performed then an action is executed`() {
        val nullableVar: Int? = null
        var isExecuted = false

        nullableVar.ifNull {
            isExecuted = true
        }

        assertTrue(isExecuted)
    }

    @Test
    fun `given a null variable when ifNonNull is performed then an action isn't executed`() {
        val nullableVar: Int? = null
        var isExecuted = false

        nullableVar.ifNotNull {
            isExecuted = true
        }

        assertFalse(isExecuted)
    }

    @Test
    fun `given a non null variable when ifNull is performed then an action isn't executed`() {
        val nullableVar: Int?
        nullableVar = 0
        var isExecuted = false

        nullableVar.ifNull {
            isExecuted = true
        }

        assertFalse(isExecuted)
    }

    @Test
    fun `given a non null variable when ifNonNull is performed then an action is executed`() {
        val nullableVar: Int?
        nullableVar = 0
        var isExecuted = false

        nullableVar.ifNotNull {
            isExecuted = true
        }

        assertTrue(isExecuted)
    }
}