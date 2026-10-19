package com.felipearpa.tyche.bet.pending

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Which relative day a Bets header names, from an injected now and time zone. The localized text
 * of each kind, in English and both Spanish locales, is covered by the instrumented
 * `PendingBetDayLabelFormatTest`, because it comes from the platform's date patterns.
 */
@OptIn(ExperimentalTime::class)
class PendingBetDayTest {
    /** Sunday, 18 October 2026. */
    private val today = LocalDate(2026, 10, 18)

    @Test
    fun `given today, tomorrow, or yesterday then the header names it`() {
        PendingBetDay.of(day = today, today = today) shouldBe PendingBetDay.Today
        PendingBetDay.of(day = LocalDate(2026, 10, 19), today = today) shouldBe PendingBetDay.Tomorrow
        PendingBetDay.of(day = LocalDate(2026, 10, 17), today = today) shouldBe PendingBetDay.Yesterday
    }

    @Test
    fun `given a day within the next six then the header names its weekday`() {
        PendingBetDay.of(day = LocalDate(2026, 10, 21), today = today) shouldBe PendingBetDay.Weekday(LocalDate(2026, 10, 21))
        PendingBetDay.of(day = LocalDate(2026, 10, 24), today = today) shouldBe PendingBetDay.Weekday(LocalDate(2026, 10, 24))
    }

    @Test
    fun `given a day a week or more away then the header is a short date without the year`() {
        PendingBetDay.of(day = LocalDate(2026, 10, 25), today = today) shouldBe PendingBetDay.Date(LocalDate(2026, 10, 25), includesYear = false)
        PendingBetDay.of(day = LocalDate(2026, 10, 30), today = today) shouldBe PendingBetDay.Date(LocalDate(2026, 10, 30), includesYear = false)
        PendingBetDay.of(day = LocalDate(2026, 10, 15), today = today) shouldBe PendingBetDay.Date(LocalDate(2026, 10, 15), includesYear = false)
    }

    @Test
    fun `given a day in another year then the short date includes the year`() {
        PendingBetDay.of(day = LocalDate(2027, 1, 2), today = today) shouldBe PendingBetDay.Date(LocalDate(2027, 1, 2), includesYear = true)
        PendingBetDay.of(day = LocalDate(2025, 12, 30), today = LocalDate(2026, 1, 2)) shouldBe
            PendingBetDay.Date(LocalDate(2025, 12, 30), includesYear = true)
    }

    @Test
    fun `given the year turns within the week then the weekday still names the day`() {
        PendingBetDay.of(day = LocalDate(2027, 1, 2), today = LocalDate(2026, 12, 30)) shouldBe PendingBetDay.Weekday(LocalDate(2027, 1, 2))
    }

    @Test
    fun `given midnight passes in the device time zone then tomorrow becomes today`() {
        val timeZone = TimeZone.of("America/Bogota")
        val kickoffDay = LocalDate(2026, 10, 19)
        val beforeMidnight = Instant.parse("2026-10-19T04:59:00Z")
        val atMidnight = Instant.parse("2026-10-19T05:00:00Z")

        PendingBetDay.of(day = kickoffDay, today = beforeMidnight.toLocalDateTime(timeZone).date) shouldBe PendingBetDay.Tomorrow
        PendingBetDay.of(day = kickoffDay, today = atMidnight.toLocalDateTime(timeZone).date) shouldBe PendingBetDay.Today
        // The same instant is already the 19th in Madrid.
        PendingBetDay.of(day = kickoffDay, today = beforeMidnight.toLocalDateTime(TimeZone.of("Europe/Madrid")).date) shouldBe
            PendingBetDay.Today
    }
}
