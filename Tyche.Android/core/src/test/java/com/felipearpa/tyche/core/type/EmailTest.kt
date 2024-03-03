package com.felipearpa.tyche.core.type

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(Parameterized::class)
class EmailTestWithValidValuesTest(private val value: String) {
    @Test
    fun `given a valid email string when an Email is created then an Email with the string contained is returned`() {
        val email = Email(value)
        assertEquals(expected = value, actual = email.value)
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
        assertFailsWith<IllegalArgumentException> { Email(value) }
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