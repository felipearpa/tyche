package com.felipearpa.tyche.pool

fun poolGamblerScorePlaceholderModel() =
    PoolGamblerScoreModel(
        poolId = "X".repeat(15),
        poolName = "X".repeat(25),
        gamblerId = "X".repeat(15),
        gamblerUsername = "X".repeat(25),
        position = 1,
        beforePosition = 1,
        score = 1,
    )

fun poolGamblerScoreDummyModel() =
    PoolGamblerScoreModel(
        poolId = "pool123",
        poolName = "Champions League",
        gamblerId = "gambler001",
        gamblerUsername = "AceBettor",
        position = 1,
        beforePosition = 2,
        score = 150,
    )

fun poolGamblerScoreWithoutScoreDummyModel() =
    PoolGamblerScoreModel(
        poolId = "pool123",
        poolName = "Champions League",
        gamblerId = "gambler001",
        gamblerUsername = "AceBettor",
        position = null,
        beforePosition = null,
        score = null,
    )

fun poolGamblerScoreDummyModels() =
    listOf(
        PoolGamblerScoreModel(
            poolId = "pool123",
            poolName = "Champions League",
            gamblerId = "gambler001",
            gamblerUsername = "AceBettor",
            position = 1,
            beforePosition = 2,
            score = 150,
        ),
        PoolGamblerScoreModel(
            poolId = "pool456",
            poolName = "American Cup",
            gamblerId = "gambler001",
            gamblerUsername = "AceBettor",
            position = null,
            beforePosition = null,
            score = null,
        ),
        PoolGamblerScoreModel(
            poolId = "pool789",
            poolName = "Premier League",
            gamblerId = "gambler002",
            gamblerUsername = "BetMaster77",
            position = 3,
            beforePosition = 1,
            score = 135,
        ),
        PoolGamblerScoreModel(
            poolId = "pool012",
            poolName = "World Cup",
            gamblerId = "gambler003",
            gamblerUsername = "LuckyStrike",
            position = 2,
            beforePosition = 2,
            score = 142,
        ),
    )
