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
        gamblerCount = 100,
    )

fun poolGamblerScoreDummyModel() =
    PoolGamblerScoreModel(
        poolId = "pool001",
        poolName = "Liga de Campeones",
        gamblerId = "gambler001",
        gamblerUsername = "AceBettor",
        position = 1,
        beforePosition = 2,
        score = 150,
        gamblerCount = 55,
    )

fun poolGamblerScoreDummyModelWithoutPosition() =
    PoolGamblerScoreModel(
        poolId = "pool001",
        poolName = "Liga de Campeones",
        gamblerId = "gambler001",
        gamblerUsername = "AceBettor",
        position = null,
        beforePosition = null,
        score = 0,
        gamblerCount = 71,
    )

fun poolGamblerScoreWithoutPositionDummyModel() =
    PoolGamblerScoreModel(
        poolId = "pool001",
        poolName = "Liga de Campeones",
        gamblerId = "gambler001",
        gamblerUsername = "AceBettor",
        position = null,
        beforePosition = null,
        score = 0,
        gamblerCount = 20,
    )

fun poolGamblerScoreDummyModels() =
    listOf(
        PoolGamblerScoreModel(
            poolId = "pool001",
            poolName = "Liga de Campeones",
            gamblerId = "gambler001",
            gamblerUsername = "ElGoleador",
            position = 1,
            beforePosition = 2,
            score = 150,
            gamblerCount = 100,
        ),
        PoolGamblerScoreModel(
            poolId = "pool002",
            poolName = "Copa Libertadores",
            gamblerId = "gambler002",
            gamblerUsername = "Pichichi_88",
            position = 5,
            beforePosition = 4,
            score = 120,
            gamblerCount = 71,
        ),
        PoolGamblerScoreModel(
            poolId = "pool003",
            poolName = "LaLiga",
            gamblerId = "gambler003",
            gamblerUsername = "AstroDelBalon",
            position = 2,
            beforePosition = 2,
            score = 200,
            gamblerCount = 98,
        ),
        PoolGamblerScoreModel(
            poolId = "pool004",
            poolName = "Copa del Mundo",
            gamblerId = "gambler004",
            gamblerUsername = "ReyDeLasApuestas",
            position = 10,
            beforePosition = 15,
            score = 80,
            gamblerCount = 35,
        ),
        PoolGamblerScoreModel(
            poolId = "pool005",
            poolName = "Copa América",
            gamblerId = "gambler005",
            gamblerUsername = "Fanatico_Futbol",
            position = 3,
            beforePosition = 1,
            score = 95,
            gamblerCount = 150,
        ),
        PoolGamblerScoreModel(
            poolId = "pool006",
            poolName = "Eurocopa",
            gamblerId = "gambler006",
            gamblerUsername = "MagoDeLaCancha",
            position = 7,
            beforePosition = 8,
            score = 110,
            gamblerCount = 111,
        ),
        PoolGamblerScoreModel(
            poolId = "pool007",
            poolName = "Bundesliga",
            gamblerId = "gambler007",
            gamblerUsername = "GoleadorEstrella",
            position = 4,
            beforePosition = 3,
            score = 140,
            gamblerCount = 26,
        ),
        PoolGamblerScoreModel(
            poolId = "pool008",
            poolName = "Serie A",
            gamblerId = "gambler008",
            gamblerUsername = "GambetaPro",
            position = 6,
            beforePosition = 6,
            score = 130,
            gamblerCount = 37,
        ),
        PoolGamblerScoreModel(
            poolId = "pool009",
            poolName = "Copa Sudamericana",
            gamblerId = "gambler009",
            gamblerUsername = "CrackTotal",
            position = 12,
            beforePosition = 10,
            score = 70,
            gamblerCount = 101,
        ),
        PoolGamblerScoreModel(
            poolId = "pool010",
            poolName = "Liga MX",
            gamblerId = "gambler010",
            gamblerUsername = "TiburonDelArea",
            position = 1,
            beforePosition = 1,
            score = 160,
            gamblerCount = 78,
        ),
    )
