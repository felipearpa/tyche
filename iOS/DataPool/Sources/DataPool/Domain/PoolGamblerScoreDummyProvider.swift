import Foundation

/// A two-match kickoff group with one result, so dummy rows show partial movement.
private let dummyTrendGroup = TrendGroup(
    kickoffDateTime: Date(timeIntervalSince1970: 1_781_283_600),
    computedMatches: 1,
    totalMatches: 2
)

func dummyPoolGamblerScore() -> PoolGamblerScore {
    PoolGamblerScore(
        poolId: "A3C2E1",
        poolName: "Neptune World Series 2023",
        gamblerId: "YF23H1",
        gamblerUsername: "neptune-player",
        position: 4,
        beforePosition: 3,
        score: 8,
        gamblerCount: 71,
        trendGroup: dummyTrendGroup
    )
}

func poolGamblerScoreDummyModels() -> [PoolGamblerScore] {
    return [
        PoolGamblerScore(
            poolId: "pool001",
            poolName: "Liga de Campeones",
            gamblerId: "gambler001",
            gamblerUsername: "ElGoleador",
            position: 1,
            beforePosition: 2,
            score: 150,
            gamblerCount: 100,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool002",
            poolName: "Copa Libertadores",
            gamblerId: "gambler002",
            gamblerUsername: "Pichichi_88",
            position: 5,
            beforePosition: 4,
            score: 120,
            gamblerCount: 100,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool003",
            poolName: "LaLiga",
            gamblerId: "gambler003",
            gamblerUsername: "AstroDelBalon",
            position: 2,
            beforePosition: 2,
            score: 200,
            gamblerCount: 111,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool004",
            poolName: "Copa del Mundo",
            gamblerId: "gambler004",
            gamblerUsername: "ReyDeLasApuestas",
            position: 10,
            beforePosition: 15,
            score: 80,
            gamblerCount: 77,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool005",
            poolName: "Copa América",
            gamblerId: "gambler005",
            gamblerUsername: "Fanatico_Futbol",
            position: 3,
            beforePosition: 1,
            score: 95,
            gamblerCount: 35,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool006",
            poolName: "Eurocopa",
            gamblerId: "gambler006",
            gamblerUsername: "MagoDeLaCancha",
            position: 7,
            beforePosition: 8,
            score: 110,
            gamblerCount: 59,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool007",
            poolName: "Bundesliga",
            gamblerId: "gambler007",
            gamblerUsername: "GoleadorEstrella",
            position: 4,
            beforePosition: 3,
            score: 140,
            gamblerCount: 89,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool008",
            poolName: "Serie A",
            gamblerId: "gambler008",
            gamblerUsername: "GambetaPro",
            position: 6,
            beforePosition: 6,
            score: 130,
            gamblerCount: 95,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool009",
            poolName: "Copa Sudamericana",
            gamblerId: "gambler009",
            gamblerUsername: "CrackTotal",
            position: 12,
            beforePosition: 10,
            score: 70,
            gamblerCount: 27,
            trendGroup: dummyTrendGroup
        ),
        PoolGamblerScore(
            poolId: "pool010",
            poolName: "Liga MX",
            gamblerId: "gambler010",
            gamblerUsername: "TiburonDelArea",
            position: 1,
            beforePosition: 1,
            score: 160,
            gamblerCount: 121,
            trendGroup: dummyTrendGroup
        )
    ]
}
