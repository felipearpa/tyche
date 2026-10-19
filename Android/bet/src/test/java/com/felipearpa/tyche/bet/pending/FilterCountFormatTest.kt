package com.felipearpa.tyche.bet.pending

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

/** Filter chip counts follow the device locale's number format, grouping separator included. */
class FilterCountFormatTest {
    private val english = Locale.forLanguageTag("en-US")
    private val colombianSpanish = Locale.forLanguageTag("es-CO")

    @Test
    fun `given US English then counts group thousands with a comma`() {
        formatFilterCount(3000, english) shouldBe "3,000"
        formatFilterCount(1123, english) shouldBe "1,123"
        formatFilterCount(42, english) shouldBe "42"
    }

    @Test
    fun `given Colombian Spanish then counts group thousands with a period`() {
        formatFilterCount(3000, colombianSpanish) shouldBe "3.000"
        formatFilterCount(1123, colombianSpanish) shouldBe "1.123"
        formatFilterCount(0, colombianSpanish) shouldBe "0"
    }
}
