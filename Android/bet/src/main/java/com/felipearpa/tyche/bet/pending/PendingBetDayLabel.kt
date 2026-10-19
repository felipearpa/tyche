package com.felipearpa.tyche.bet.pending

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.bet.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A kickoff day relative to today, as a Bets date header names it. */
internal sealed interface PendingBetDay {
    data object Today : PendingBetDay
    data object Tomorrow : PendingBetDay
    data object Yesterday : PendingBetDay

    /** One of the next six days after tomorrow, named by its weekday. */
    data class Weekday(val day: LocalDate) : PendingBetDay

    /** Any other day, as a short named date, with the year when it is not the current one. */
    data class Date(val day: LocalDate, val includesYear: Boolean) : PendingBetDay

    companion object {
        fun of(day: LocalDate, today: LocalDate): PendingBetDay = when (today.daysUntil(day)) {
            0 -> Today
            1 -> Tomorrow
            -1 -> Yesterday
            in 2..WEEKDAY_HORIZON -> Weekday(day)
            else -> Date(day, includesYear = day.year != today.year)
        }
    }
}

private const val WEEKDAY_HORIZON = 6

/**
 * Formats [PendingBetDay]s in [locale] with the same platform formatters History's headers use,
 * capitalized as a heading ("Miércoles", not "miércoles"). Today, Tomorrow, and Yesterday are the
 * app's own strings, [relativeNames], because the platform has no relative-day name.
 */
internal class PendingBetDayLabelFormat(
    private val locale: Locale,
    private val relativeNames: RelativeDayNames,
) {
    data class RelativeDayNames(val today: String, val tomorrow: String, val yesterday: String)

    private val weekday = formatter(WEEKDAY_SKELETON)
    private val shortDate = formatter(SHORT_DATE_SKELETON)
    private val shortDateWithYear = formatter(SHORT_DATE_WITH_YEAR_SKELETON)

    fun text(day: PendingBetDay): String = when (day) {
        PendingBetDay.Today -> relativeNames.today
        PendingBetDay.Tomorrow -> relativeNames.tomorrow
        PendingBetDay.Yesterday -> relativeNames.yesterday
        is PendingBetDay.Weekday -> weekday.format(day.day.toJavaLocalDate()).asHeading()
        is PendingBetDay.Date -> (if (day.includesYear) shortDateWithYear else shortDate)
            .format(day.day.toJavaLocalDate())
            .asHeading()
    }

    private fun String.asHeading(): String = replaceFirstChar { char -> char.titlecase(locale) }

    private fun formatter(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

private const val WEEKDAY_SKELETON = "EEEE"
private const val SHORT_DATE_SKELETON = "EEEMMMd"
private const val SHORT_DATE_WITH_YEAR_SKELETON = "EEEMMMdy"

/** The day-header format for the current locale. */
@Composable
internal fun rememberPendingBetDayLabelFormat(): PendingBetDayLabelFormat {
    val locale = LocalLocale.current.platformLocale
    val names = PendingBetDayLabelFormat.RelativeDayNames(
        today = stringResource(R.string.bet_day_today),
        tomorrow = stringResource(R.string.bet_day_tomorrow),
        yesterday = stringResource(R.string.bet_day_yesterday),
    )
    return remember(locale, names) { PendingBetDayLabelFormat(locale = locale, relativeNames = names) }
}
