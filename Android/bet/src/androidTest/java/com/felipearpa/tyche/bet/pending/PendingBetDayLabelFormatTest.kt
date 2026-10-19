package com.felipearpa.tyche.bet.pending

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.Test
import java.util.Locale

/**
 * The text of each kind of Bets day header, formatted with the platform's date patterns in English,
 * Spanish, and Spain Spanish. Which kind a day is comes from `PendingBetDayTest`.
 */
class PendingBetDayLabelFormatTest {
    private val english = PendingBetDayLabelFormat(Locale.US, PendingBetDayLabelFormat.RelativeDayNames("Today", "Tomorrow", "Yesterday"))
    private val spanishNames = PendingBetDayLabelFormat.RelativeDayNames("Hoy", "Mañana", "Ayer")
    private val spanish = PendingBetDayLabelFormat(Locale.forLanguageTag("es-CO"), spanishNames)
    private val spainSpanish = PendingBetDayLabelFormat(Locale.forLanguageTag("es-ES"), spanishNames)

    private val wednesday = PendingBetDay.Weekday(LocalDate(2026, 10, 21))
    private val friday30 = PendingBetDay.Date(LocalDate(2026, 10, 30), includesYear = false)
    private val nextYear = PendingBetDay.Date(LocalDate(2027, 1, 2), includesYear = true)

    @Test
    fun englishNamesRelativeDaysWeekdaysAndShortDates() {
        english.text(PendingBetDay.Today) shouldBe "Today"
        english.text(PendingBetDay.Tomorrow) shouldBe "Tomorrow"
        english.text(PendingBetDay.Yesterday) shouldBe "Yesterday"
        english.text(wednesday) shouldBe "Wednesday"
        english.text(friday30) shouldBe "Fri, Oct 30"
        english.text(nextYear) shouldBe "Sat, Jan 2, 2027"
    }

    @Test
    fun spanishCapitalizesWeekdaysAndDatesAsHeadings() {
        spanish.text(PendingBetDay.Today) shouldBe "Hoy"
        spanish.text(PendingBetDay.Tomorrow) shouldBe "Mañana"
        spanish.text(wednesday) shouldBe "Miércoles"
        spanish.text(friday30) shouldBe "Vie, 30 de oct"
        spanish.text(nextYear) shouldBe "Sáb, 2 ene 2027"
    }

    @Test
    fun spainSpanishUsesItsOwnPatterns() {
        spainSpanish.text(PendingBetDay.Yesterday) shouldBe "Ayer"
        spainSpanish.text(wednesday) shouldBe "Miércoles"
        spainSpanish.text(friday30) shouldBe "Vie, 30 oct"
        spainSpanish.text(nextYear) shouldBe "Sáb, 2 ene 2027"
    }
}
