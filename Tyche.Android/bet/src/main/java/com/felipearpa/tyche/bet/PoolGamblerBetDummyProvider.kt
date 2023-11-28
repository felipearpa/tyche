package com.felipearpa.tyche.bet

import com.felipearpa.tyche.core.type.TeamScore
import java.time.LocalDateTime

fun poolGamblerBetFakeModel() =
    PoolGamblerBetModel(
        poolId = "X".repeat(15),
        gamblerId = "X".repeat(15),
        matchId = "X".repeat(15),
        homeTeamId = "X".repeat(15),
        homeTeamName = "X".repeat(25),
        awayTeamId = "X".repeat(15),
        awayTeamName = "X".repeat(25),
        matchScore = TeamScore(10, 10),
        betScore = TeamScore(10, 10),
        score = 10,
        matchDateTime = LocalDateTime.now(),
        isLocked = true
    )

fun poolGamblerBetDummyModel() =
    PoolGamblerBetModel(
        poolId = "pool123",
        gamblerId = "gambler456",
        matchId = "match789",
        homeTeamId = "team101",
        homeTeamName = "Team A",
        awayTeamId = "team202",
        awayTeamName = "Team B",
        matchScore = TeamScore(2, 1),
        betScore = TeamScore(1, 2),
        score = 10,
        matchDateTime = LocalDateTime.now(),
        isLocked = false
    )