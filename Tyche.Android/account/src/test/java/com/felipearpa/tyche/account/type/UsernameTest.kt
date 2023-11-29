package com.felipearpa.tyche.account.type

import com.felipearpa.session.type.Username
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UsernameWithValidValuesTest(private val value: String) {

    @Test
    fun `given a valid username string when an Username is created then an Username with the string contained is returned`() {
        val username = Username(value)
        assertEquals(value, username.value)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("felipearpa"),
                arrayOf("felipearpa-1"),
                arrayOf("felipearpa_1")
            )
        }
    }

}

@RunWith(Parameterized::class)
class UsernameWithInvalidValuesTest(private val value: String) {

    @Test
    fun `given an invalid username string when an Username is created then an exception is raised`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { Username(value) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(""),
                arrayOf("mi"),
                arrayOf("felipearpa."),
                arrayOf("felipearpa#"),
                arrayOf("usernameverylong1")
            )
        }
    }
}