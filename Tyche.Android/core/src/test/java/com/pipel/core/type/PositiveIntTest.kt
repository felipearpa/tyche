package com.pipel.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PositiveIntTest {

    @Test
    fun `given a positive int when a PositiveInt is created then a Positive than contains the value is returned`() {
        val source = 1
        val positiveInt = PositiveInt(source)
        assertEquals(source, positiveInt.value)
    }

    @Test
    fun `given a non positive number when a PositiveInt is created then an exception is raised`() {
        val source = -1
        assertThrows(
            IllegalArgumentException::class.java
        ) { PositiveInt(source) }
    }

}