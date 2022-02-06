package com.pipel.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UlidTest {

    @Test
    fun `given a valid ULID string when a Ulid is created then a Ulid than contains the string value is returned`() {
        val sourceString = "01FWSRVQ503BGCAY09WPSTGZ0N"
        val ulid = Ulid(sourceString)
        assertEquals(sourceString, ulid.value)
    }

    @Test
    fun `given an invalid ULID string when a Ulid is created then an exception is raised`() {
        val sourceString = "null"
        assertThrows(
            IllegalArgumentException::class.java
        ) { Ulid(sourceString) }
    }

}