package com.felipearpa.tyche.bet.finished

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The date and time shown in a History or Timeline kickoff-group header and the match screen
 * heading. The date names the weekday, abbreviated month, and day in the locale's order, and adds
 * the year only when the match falls outside the current calendar year. The time follows the
 * locale and the device's 12/24-hour setting.
 *
 * A kickoff instant is shown in [timeZone]. A [LocalDateTime] match time arrives already
 * converted to the device's time zone. Formatted values are labels only: two different kickoffs
 * can read alike, for example when daylight saving time ends, so nothing is grouped by them.
 */
class HistoryMatchDateFormat(
    private val locale: Locale,
    private val is24HourClock: Boolean,
    private val today: LocalDate,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val currentYearDate = formatter(DATE_SKELETON)
    private val otherYearDate = formatter(DATE_WITH_YEAR_SKELETON)
    private val time = formatter(if (is24HourClock) TIME_24_HOUR_SKELETON else TIME_12_HOUR_SKELETON)

    fun date(dateTime: LocalDateTime): String {
        val formatter = if (dateTime.year == today.year) currentYearDate else otherYearDate
        return formatter.format(dateTime.toJavaLocalDateTime())
    }

    fun time(dateTime: LocalDateTime): String = time.format(dateTime.toJavaLocalDateTime())

    fun date(instant: Instant): String = date(instant.toLocalDateTime(timeZone))

    fun time(instant: Instant): String = time(instant.toLocalDateTime(timeZone))

    private fun formatter(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

private const val DATE_SKELETON = "EEEEMMMd"
private const val DATE_WITH_YEAR_SKELETON = "EEEEMMMdy"
private const val TIME_12_HOUR_SKELETON = "hm"
private const val TIME_24_HOUR_SKELETON = "Hm"

/** History's date format for the current locale, clock setting, and day. */
@OptIn(ExperimentalTime::class)
@Composable
internal fun rememberHistoryMatchDateFormat(): HistoryMatchDateFormat {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    val is24HourClock = DateFormat.is24HourFormat(context)
    val timeZone = TimeZone.currentSystemDefault()
    val today = Clock.System.todayIn(timeZone)
    return remember(locale, is24HourClock, today, timeZone) {
        HistoryMatchDateFormat(locale = locale, is24HourClock = is24HourClock, today = today, timeZone = timeZone)
    }
}
