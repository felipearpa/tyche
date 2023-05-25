package com.felipearpa.core.type

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class EmailTestWithValidValuesTest(private val value: String) {

    @Test
    fun `given a valid email string when an Email is created then an Email with the string contained is returned`() {
        val email = Email(value)
        assertEquals(value, email.value)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("felipearpa@felipearpa.com"),
                arrayOf("felipearpa@felipearpa.com.co")
            )
        }
    }
}

@RunWith(Parameterized::class)
class EmailTestWithInvalidValuesTest(private val value: String) {

    @Test
    fun `given an invalid email string when an Email is created then an exception is raised`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { Email(value) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(""),
                arrayOf("felipearpa")
            )
        }
    }
}