package com.felipearpa.tyche.bet.pending

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** The "Locks in N min" countdown and the minute clock behind it. */
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class PendingBetClockTest {
    private val timeZone = TimeZone.of("America/Bogota")
    private val kickoff = LocalDateTime(year = 2026, month = 10, day = 21, hour = 14, minute = 0)
    private val lock = kickoff.toInstant(timeZone) - 10.minutes

    private fun countdownAt(beforeLock: Duration) =
        lockCountdownMinutes(kickoff = kickoff, now = lock - beforeLock, timeZone = timeZone)

    @Test
    fun `given the lock an hour away then the note shows 60 minutes`() {
        countdownAt(60.minutes) shouldBe 60
    }

    @Test
    fun `given the lock more than an hour away then there is no note`() {
        countdownAt(60.minutes + 1.seconds) shouldBe null
    }

    @Test
    fun `given a minute partly elapsed then the count rounds up`() {
        countdownAt(59.minutes + 59.seconds) shouldBe 60
        countdownAt(59.minutes) shouldBe 59
    }

    @Test
    fun `given the last minute then the note shows 1 minute until the lock`() {
        countdownAt(1.minutes) shouldBe 1
        countdownAt(59.seconds) shouldBe 1
        countdownAt(1.seconds) shouldBe 1
        countdownAt(1.milliseconds) shouldBe 1
    }

    @Test
    fun `given the lock instant or later then there is no note`() {
        countdownAt(Duration.ZERO) shouldBe null
        countdownAt((-1).seconds) shouldBe null
        countdownAt((-30).minutes) shouldBe null
    }

    @Test
    fun `given the clock starts mid-minute then it ticks now and at each whole minute`() = runTest {
        val start = Instant.parse("2026-10-21T18:49:20.500Z")
        val ticks = collectTicks(start)

        runCurrent()
        advanceTimeBy(39_499)
        runCurrent()
        ticks() shouldBe listOf(start)

        advanceTimeBy(1)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        ticks() shouldBe listOf(
            start,
            Instant.parse("2026-10-21T18:50:00Z"),
            Instant.parse("2026-10-21T18:51:00Z"),
        )
    }

    @Test
    fun `given a tick then the countdown changes exactly on it`() = runTest {
        // Five minutes and twenty seconds before the lock.
        val start = lock - 5.minutes - 20.seconds
        val ticks = collectTicks(start)
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()

        ticks().map { now -> lockCountdownMinutes(kickoff, now, timeZone) } shouldBe listOf(6, 5)
    }

    /** Collects [minuteTicks] on virtual time from [start]; returns a reader of the ticks so far. */
    private fun TestScope.collectTicks(start: Instant): () -> List<Instant> {
        val clock = object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }
        var ticks = emptyList<Instant>()
        backgroundScope.launch { minuteTicks(clock).collect { tick -> ticks = ticks + tick } }
        return { ticks }
    }
}
