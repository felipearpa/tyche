package com.felipearpa.tyche.core.type

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UlidTestWithValidValuesTest(private val value: String) {

    @Test
    fun `given a valid ulid string when an Ulid is created then an Ulid with the string contained is returned`() {
        val ulid = Ulid(value)
        assertEquals(value, ulid.value)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("01FWSRVQ503BGCAY09WPSTGZ0N"),
            )
        }
    }
}

@RunWith(Parameterized::class)
class UlidTestWithInvalidValuesTest(private val value: String) {

    @Test
    fun `given an invalid ulid string when an Ulid is created then an exception is raised`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { Ulid(value) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(""),
                arrayOf("null")
            )
        }
    }
}