package com.pipel.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NonEmptyStringTest {

    @Test
    fun `given a not empty string when a NonEmptyString is created then a NonEmptyString than contains the string value is returned`() {
        val sourceRaw = "not empty string"
        val nonEmptyString = NonEmptyString(sourceRaw)
        assertEquals(sourceRaw, nonEmptyString.value)
    }

    @Test
    fun `given an empty string when a NonEmptyString is created then an exception is raised`() {
        val sourceRaw = ""
        assertThrows(
            IllegalArgumentException::class.java
        ) { NonEmptyString(sourceRaw) }
    }

}