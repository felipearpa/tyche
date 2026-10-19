package com.felipearpa.tyche.bet.pending

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Predictions lock this long before kickoff, as the server enforces. */
private val LOCK_LEAD_TIME = 10.minutes

/** The "Locks in N min" note shows during this last stretch before the lock. */
private val LOCK_NOTICE_WINDOW = 60.minutes

/**
 * Whole minutes until predictions lock, rounded up; null unless the lock is in the next sixty
 * minutes. It reaches null at the lock instant, so the card never shows a minute that has
 * already passed. [kickoff] is a wall-clock time in [timeZone].
 */
@OptIn(ExperimentalTime::class)
internal fun lockCountdownMinutes(kickoff: LocalDateTime, now: Instant, timeZone: TimeZone): Int? {
    val remaining = kickoff.toInstant(timeZone) - LOCK_LEAD_TIME - now
    if (remaining <= Duration.ZERO || remaining > LOCK_NOTICE_WINDOW) return null
    val wholeMinute = 1.minutes.inWholeMilliseconds
    return ((remaining.inWholeMilliseconds + wholeMinute - 1) / wholeMinute).toInt()
}

/**
 * The current instant, then each whole minute of [clock] as it arrives, for as long as it is
 * collected. Kickoff times and midnight fall on whole minutes, so the lock note and the day
 * headers change exactly when a tick lands. A tick never reports an instant before its minute,
 * so a timer that wakes early cannot keep a minute that has passed.
 */
@OptIn(ExperimentalTime::class)
internal fun minuteTicks(clock: Clock): Flow<Instant> = flow {
    var now = clock.now()
    while (true) {
        emit(now)
        val nextMinute = Instant.fromEpochSeconds(Math.floorDiv(now.epochSeconds, SECONDS_PER_MINUTE) * SECONDS_PER_MINUTE + SECONDS_PER_MINUTE)
        delay(nextMinute - clock.now())
        now = maxOf(clock.now(), nextMinute)
    }
}

private const val SECONDS_PER_MINUTE = 60L
