package com.felipearpa.tyche.core.type

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NonEmptyStringValidValuesTest(private val value: String) {

    @Test
    fun `given a valid string when a NonEmptyString is created then a NonEmptyString with string contained is returned`() {
        val nonEmptyString = NonEmptyString(value)
        assertEquals(value, nonEmptyString.value)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("felipearpa"),
            )
        }
    }
}

@RunWith(Parameterized::class)
class NonEmptyStringTestWithInvalidValuesTest(private val value: String) {

    @Test
    fun `given an invalid string when a NonEmptyString is created then an exception is raised`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { NonEmptyString(value) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(""),
                arrayOf(" ")
            )
        }
    }
}