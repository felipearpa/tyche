package com.felipearpa.tyche.bet.pending

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** What a score box keeps from typed text. */
class BetScoreInputTest {
    @Test
    fun `given digits then the score is their value`() {
        parseBetScoreInput("2") shouldBe 2
        parseBetScoreInput("12") shouldBe 12
    }

    @Test
    fun `given no digits then there is no score, not zero`() {
        parseBetScoreInput("") shouldBe null
        parseBetScoreInput("a-") shouldBe null
    }

    @Test
    fun `given non-digits then only the digits count`() {
        parseBetScoreInput("1,") shouldBe 1
        parseBetScoreInput(" 3.") shouldBe 3
        parseBetScoreInput("٣") shouldBe null
    }

    @Test
    fun `given leading zeros then they are removed`() {
        parseBetScoreInput("0") shouldBe 0
        parseBetScoreInput("00") shouldBe 0
        parseBetScoreInput("05") shouldBe 5
    }

    @Test
    fun `given more than two digits then the first two are kept`() {
        parseBetScoreInput("123") shouldBe 12
        parseBetScoreInput("0123") shouldBe 12
    }

    @Test
    fun `given each appearance then it follows value, neighbor, and focus`() {
        BetScoreBoxAppearance.of(value = null, otherValue = null, isFocused = false) shouldBe BetScoreBoxAppearance.EMPTY
        BetScoreBoxAppearance.of(value = null, otherValue = 2, isFocused = false) shouldBe BetScoreBoxAppearance.MISSING
        BetScoreBoxAppearance.of(value = 2, otherValue = null, isFocused = false) shouldBe BetScoreBoxAppearance.FILLED
        BetScoreBoxAppearance.of(value = null, otherValue = 2, isFocused = true) shouldBe BetScoreBoxAppearance.FOCUSED
    }
}
