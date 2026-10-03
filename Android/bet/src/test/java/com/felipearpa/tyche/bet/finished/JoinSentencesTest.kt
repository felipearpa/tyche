package com.felipearpa.tyche.bet.finished

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JoinSentencesTest {
    @Test
    fun `parts are separated by a period and a space`() {
        joinSentences(listOf("Sunday, Jul 19, 2:00 PM", "Your bet: 2 to 1", "2 points")) shouldBe
            "Sunday, Jul 19, 2:00 PM. Your bet: 2 to 1. 2 points"
    }

    @Test
    fun `a part that already ends with a period gets no second one`() {
        joinSentences(listOf("domingo, 19 de jul., 2:00 p. m.", "Tu apuesta: 3 a 2")) shouldBe
            "domingo, 19 de jul., 2:00 p. m. Tu apuesta: 3 a 2"
    }
}
