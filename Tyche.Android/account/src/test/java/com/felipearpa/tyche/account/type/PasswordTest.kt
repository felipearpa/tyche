package com.felipearpa.tyche.account.type

import com.felipearpa.session.type.Password
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PasswordWithValidValuesTest(private val value: String) {

    @Test
    fun `given a valid password string when a Password is created then a Password with the string contained is returned`() {
        val password = Password(value)
        assertEquals(value, password.value)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Felipearpa1#"),
                arrayOf("#felipeArpa1"),
                arrayOf("1Felipearpa#")
            )
        }
    }

}

@RunWith(Parameterized::class)
class PasswordWithInvalidValuesTest(private val value: String) {

    @Test
    fun `given an invalid password string when a Password is created then an exception is raised`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { Password(value) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(""),
                arrayOf("felipe"),
                arrayOf("Felipe"),
                arrayOf("Felipe#"),
                arrayOf("Felipe1")
            )
        }
    }
}