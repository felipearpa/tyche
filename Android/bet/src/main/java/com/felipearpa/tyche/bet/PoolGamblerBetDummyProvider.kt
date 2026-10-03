package com.felipearpa.tyche.bet

import com.felipearpa.tyche.core.type.TeamScore
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val countries = listOf(
    "ar" to "Argentina",
    "at" to "Austria",
    "au" to "Australia",
    "ba" to "Bosnia and Herzegovina",
    "be" to "Belgium",
    "br" to "Brazil",
    "ca" to "Canada",
    "cd" to "Congo",
    "ch" to "Switzerland",
    "ci" to "Ivory Coast",
    "co" to "Colombia",
    "cv" to "Cape Verde",
    "cw" to "Curaçao",
    "cz" to "Czech Republic",
    "de" to "Germany",
    "dz" to "Algeria",
    "ec" to "Ecuador",
    "eg" to "Egypt",
    "es" to "Spain",
    "fr" to "France",
    "gb_eng" to "England",
    "gb_sct" to "Scotland",
    "gh" to "Ghana",
    "hr" to "Croatia",
    "ht" to "Haiti",
    "iq" to "Iraq",
    "ir" to "Iran",
    "jo" to "Jordan",
    "jp" to "Japan",
    "kr" to "South Korea",
    "ma" to "Morocco",
    "mx" to "Mexico",
    "nl" to "Netherlands",
    "no" to "Norway",
    "nz" to "New Zealand",
    "pa" to "Panama",
    "pt" to "Portugal",
    "py" to "Paraguay",
    "qa" to "Qatar",
    "sa" to "Saudi Arabia",
    "se" to "Sweden",
    "sn" to "Senegal",
    "tn" to "Tunisia",
    "tr" to "Turkey",
    "us" to "USA",
    "uy" to "Uruguay",
    "uz" to "Uzbekistan",
    "za" to "South Africa",
)

/**
 * Presentation-only filler for loading rows. Its kickoff time is fixed so the masked date and
 * time keep the same length on every redraw.
 */
fun poolGamblerBetFakeModel() =
    PoolGamblerBetModel(
        poolId = "X".repeat(15),
        gamblerId = "X".repeat(15),
        gamblerUsername = "X".repeat(25),
        matchId = "X".repeat(15),
        homeTeamId = "co",
        homeTeamName = "X".repeat(25),
        awayTeamId = "br",
        awayTeamName = "X".repeat(25),
        matchScore = TeamScore(100, 100),
        betScore = TeamScore(100, 100),
        score = 10,
        matchDateTime = LocalDateTime(year = 2026, month = 1, day = 1, hour = 12, minute = 0),
        isLocked = true,
        isComputed = false,
    )

@OptIn(ExperimentalTime::class)
fun poolGamblerBetDummyModel(): PoolGamblerBetModel {
    val homeCountry = countries.random()
    var awayCountry = countries.random()
    while (awayCountry == homeCountry) {
        awayCountry = countries.random()
    }
    val isComputed = (0..1).random() == 1
    val isLocked = isComputed || (0..1).random() == 1

    return PoolGamblerBetModel(
        poolId = "pool123",
        gamblerId = "gambler456",
        gamblerUsername = "gambler456@example.com",
        matchId = "match789",
        homeTeamId = homeCountry.first,
        homeTeamName = homeCountry.second,
        awayTeamId = awayCountry.first,
        awayTeamName = awayCountry.second,
        matchScore = if (isComputed) TeamScore((0..5).random(), (0..5).random()) else null,
        betScore = TeamScore((0..5).random(), (0..5).random()),
        score = if (isComputed) (0..30).random() else null,
        matchDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        isLocked = isLocked,
        isComputed = isComputed,
    )
}

@OptIn(ExperimentalTime::class)
fun poolGamblerBetDummyModels(): List<PoolGamblerBetModel> =
    (1..10).map { i ->
        val homeCountry = countries.random()
        var awayCountry = countries.random()
        while (awayCountry == homeCountry) {
            awayCountry = countries.random()
        }
        val isComputed = (0..1).random() == 1
        val isLocked = isComputed || (0..1).random() == 1

        PoolGamblerBetModel(
            poolId = "pool${(100..999).random()}",
            gamblerId = "gambler${(100..999).random()}",
            gamblerUsername = "gambler${i}@example.com",
            matchId = "match${(100..999).random()}",
            homeTeamId = homeCountry.first,
            homeTeamName = homeCountry.second,
            awayTeamId = awayCountry.first,
            awayTeamName = awayCountry.second,
            matchScore = if (isComputed) TeamScore((0..5).random(), (0..5).random()) else null,
            betScore = TeamScore((0..5).random(), (0..5).random()),
            score = (0..10).random(),
            matchDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isLocked = isLocked,
            isComputed = isComputed,
        )
    }

@OptIn(ExperimentalTime::class)
fun poolGamblerBetFinishedDummyModels(): List<PoolGamblerBetModel> =
    (1..10).map { i ->
        val homeCountry = countries.random()
        var awayCountry = countries.random()
        while (awayCountry == homeCountry) {
            awayCountry = countries.random()
        }

        PoolGamblerBetModel(
            poolId = "pool${(100..999).random()}",
            gamblerId = "gambler${(100..999).random()}",
            gamblerUsername = "gambler${i}@example.com",
            matchId = "match${(100..999).random()}",
            homeTeamId = homeCountry.first,
            homeTeamName = homeCountry.second,
            awayTeamId = awayCountry.first,
            awayTeamName = awayCountry.second,
            matchScore = TeamScore((0..5).random(), (0..5).random()),
            betScore = TeamScore((0..5).random(), (0..5).random()),
            score = (0..10).random(),
            matchDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isLocked = true,
            isComputed = true,
        )
    }

@OptIn(ExperimentalTime::class)
fun poolGamblerBetPendingDummyModels(): List<PoolGamblerBetModel> =
    (1..10).map { i ->
        val homeCountry = countries.random()
        var awayCountry = countries.random()
        while (awayCountry == homeCountry) {
            awayCountry = countries.random()
        }
        val isLocked = (0..1).random() == 1

        PoolGamblerBetModel(
            poolId = "pool${(100..999).random()}",
            gamblerId = "gambler${(100..999).random()}",
            gamblerUsername = "gambler${i}@example.com",
            matchId = "match${(100..999).random()}",
            homeTeamId = homeCountry.first,
            homeTeamName = homeCountry.second,
            awayTeamId = awayCountry.first,
            awayTeamName = awayCountry.second,
            matchScore = null,
            betScore = TeamScore((0..5).random(), (0..5).random()),
            score = (0..10).random(),
            matchDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            isLocked = isLocked,
            isComputed = false,
        )
    }

/**
 * History preview rows covering a positive award, a zero award, a missing bet, a long team name,
 * unavailable result and points, and a match from an earlier year.
 */
fun historyBetPreviewModels(): List<PoolGamblerBetModel> {
    val base = poolGamblerBetFinishedDummyModels().first().copy(
        matchDateTime = LocalDateTime(year = 2026, month = 7, day = 19, hour = 14, minute = 0),
    )
    return listOf(
        base.copy(
            matchId = "history-positive",
            homeTeamId = "gb_eng",
            homeTeamName = "Inglaterra",
            awayTeamId = "ar",
            awayTeamName = "Argentina",
            matchScore = TeamScore(1, 2),
            betScore = TeamScore(2, 1),
            score = 2,
        ),
        base.copy(
            matchId = "history-zero",
            homeTeamId = "es",
            homeTeamName = "España",
            awayTeamId = "ar",
            awayTeamName = "Argentina",
            matchScore = TeamScore(0, 0),
            betScore = TeamScore(3, 2),
            score = 0,
        ),
        base.copy(
            matchId = "history-long-name",
            homeTeamId = "pt",
            homeTeamName = "Portugal",
            awayTeamId = "cd",
            awayTeamName = "República Democrática del Congo",
            matchScore = TeamScore(1, 1),
            betScore = null,
            score = 1,
        ),
        base.copy(
            matchId = "history-unavailable",
            homeTeamId = "us",
            homeTeamName = "Estados Unidos",
            awayTeamId = "py",
            awayTeamName = "Paraguay",
            matchScore = null,
            betScore = null,
            score = null,
            matchDateTime = LocalDateTime(year = 2025, month = 6, day = 11, hour = 20, minute = 0),
        ),
    )
}

/**
 * Stable filler for History's row placeholders, sized like a typical finished match so loading
 * rows keep the loaded rows' height. Never shown as real content.
 */
fun historyBetPlaceholderModel() =
    poolGamblerBetFakeModel().copy(
        homeTeamName = "X".repeat(8),
        awayTeamName = "X".repeat(8),
        matchScore = TeamScore(0, 0),
        betScore = TeamScore(0, 0),
        score = 0,
        isComputed = true,
    )
