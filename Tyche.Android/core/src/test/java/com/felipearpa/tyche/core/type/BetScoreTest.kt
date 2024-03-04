package com.felipearpa.tyche.core.type

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class BetScoreTest {
    @TestFactory
    fun `given a valid value when a BetScore is created then the BetScore is returned`() =
        listOf(0, 500, 999).map { rawBetScore ->
            dynamicTest("given $rawBetScore when a BetScore is created then the BetScore is returned") {
                val betScore = BetScore(rawBetScore)
                betScore.value shouldBe rawBetScore
            }
        }

    @TestFactory
    fun `given an invalid value when a BetScore is created then an exception is raised`() =
        listOf(-1, 1000).map { rawBetScore ->
            dynamicTest("given $rawBetScore when a BetScore is created then an exception is raised") {
                shouldThrow<IllegalArgumentException> {
                    BetScore(rawBetScore)
                }
            }
        }
}