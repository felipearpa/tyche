package com.felipearpa.tyche.core.network

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class HttpStatusCodeTest {
    @TestFactory
    fun `given numeric status code when is transformed to HttpStatusCode then correct HttpStatusCode is returned`() =
        HttpStatusCode.entries.map { enum -> Pair(enum.value, enum) }.toList()
            .map { (numericHttpStatusCode, httpStatusCode) ->
                dynamicTest("given $numericHttpStatusCode when is transformed to HttpStatusCode then $httpStatusCode is returned") {
                    val actualHttpStatusCode = numericHttpStatusCode.toHtpStatusCode()
                    actualHttpStatusCode shouldBe httpStatusCode
                }
            }
}