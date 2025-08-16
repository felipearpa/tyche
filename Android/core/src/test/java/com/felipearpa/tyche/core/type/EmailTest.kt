package com.felipearpa.tyche.core.type

import com.felipearpa.tyche.core.emptyString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class EmailTest {
    @TestFactory
    fun `given a valid string when an Email is created then an Email containing the string is returned`() =
        listOf("mail@felipearpa.com", "mail@felipearpa.com.co").map { rawEmail ->
            dynamicTest("given $rawEmail when en Email is created then an Email containing the string is returned") {
                val email = Email(rawEmail)
                email.value shouldBe rawEmail
            }
        }

    @TestFactory
    fun `given an invalid string when an Email is created then an exception is raised`() =
        listOf(emptyString(), "invalid").map { rawEmail ->
            dynamicTest("given $rawEmail when an Email is created then an exception is raised") {
                shouldThrow<IllegalArgumentException> {
                    Email(rawEmail)
                }
            }
        }
}