import Foundation

func poolGamblerScores() -> [PoolGamblerScore] {
    return [
        PoolGamblerScore(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            currentPosition: 4,
            beforePosition: 3,
            score: 8
        ),
        PoolGamblerScore(
            poolId: "X4D7B9",
            poolName: "Ares Euro Championship 2024",
            gamblerId: "ZT94Q5",
            gamblerUsername: "ares-bettor",
            currentPosition: 2,
            beforePosition: nil,
            score: 10
        ),
        PoolGamblerScore(
            poolId: "J1K5M7",
            poolName: "Hermes International Cup 2023",
            gamblerId: "XN28J4",
            gamblerUsername: "hermes-punter",
            currentPosition: 1,
            beforePosition: 4,
            score: 6
        ),
        PoolGamblerScore(
            poolId: "P6Z8V4",
            poolName: "Athena Olympics 2024",
            gamblerId: "M9L2S7",
            gamblerUsername: "athena-challenger",
            currentPosition: nil,
            beforePosition: nil,
            score: nil
        )
    ]
}

func poolGamblerScoreModel() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: "A3C2E1",
        poolName: "Neptune World Series 2023",
        gamblerId: "YF23H1",
        gamblerUsername: "neptune-player",
        currentPosition: 4,
        beforePosition: 3,
        score: 8
    )
}

func fakePoolGamblerScoreModel() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: UUID().uuidString,
        poolName: String(repeating: "X", count: 25),
        gamblerId: UUID().uuidString,
        gamblerUsername: String(repeating: "X", count: 15),
        currentPosition: 1,
        beforePosition: 2,
        score: 10
    )
}

func poolGamblerScoreModels() -> [PoolGamblerScoreModel] {
    return poolGamblerScores().map { poolGamblerScore in
        poolGamblerScore.toPoolGamblerScoreModel()
    }
}
