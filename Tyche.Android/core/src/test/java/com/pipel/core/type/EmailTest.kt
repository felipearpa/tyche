package com.pipel.core.type

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EmailTest {

    @Test
    fun `given a valid email string when an Email is created then an Email than contains the string value is returned`() {
        val sourceRaw = "email@email.com"
        val email = Email(sourceRaw)
        assertEquals(sourceRaw, email.value)
    }

    @ParameterizedTest
    @MethodSource("valuesForInvalidEmail")
    fun `given an invalid email string when an Email is created then an exception is raised`(
        sourceRaw: String
    ) {
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { Email(sourceRaw) }
    }

    companion object {
        @JvmStatic
        fun valuesForInvalidEmail(): Array<Array<String>> {
            return arrayOf(arrayOf(""), arrayOf("invalid"))
        }
    }

}