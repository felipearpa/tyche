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
        matchScore = TeamScore(100, 100),
        betScore = TeamScore(100, 100),
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

fun poolGamblerBetDummyModels() =
    listOf(
        PoolGamblerBetModel(
            poolId = "pool001",
            gamblerId = "gambler123",
            matchId = "match321",
            homeTeamId = "team111",
            homeTeamName = "Red Dragons",
            awayTeamId = "team222",
            awayTeamName = "Blue Hawks",
            matchScore = TeamScore(3, 0),
            betScore = TeamScore(2, 1),
            score = 15,
            matchDateTime = LocalDateTime.now(),
            isLocked = true
        ),
        PoolGamblerBetModel(
            poolId = "pool002",
            gamblerId = "gambler456",
            matchId = "match654",
            homeTeamId = "team333",
            homeTeamName = "Green Eagles",
            awayTeamId = "team444",
            awayTeamName = "Yellow Tigers",
            matchScore = TeamScore(1, 1),
            betScore = TeamScore(0, 1),
            score = 20,
            matchDateTime = LocalDateTime.now(),
            isLocked = false
        ),
        PoolGamblerBetModel(
            poolId = "pool003",
            gamblerId = "gambler789",
            matchId = "match987",
            homeTeamId = "team555",
            homeTeamName = "Black Panthers",
            awayTeamId = "team666",
            awayTeamName = "White Wolves",
            matchScore = TeamScore(2, 2),
            betScore = TeamScore(3, 1),
            score = 25,
            matchDateTime = LocalDateTime.now().plusDays(1),
            isLocked = true
        )
    )