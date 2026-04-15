import Foundation

public func poolGamblerScorePlaceholderModel() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: UUID().uuidString,
        poolName: String(repeating: "X", count: 25),
        gamblerId: UUID().uuidString,
        gamblerUsername: String(repeating: "X", count: 15),
        position: 1,
        beforePosition: 2,
        score: 10
    )
}

public func poolGamblerScoreDummyModel() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: "A3C2E1",
        poolName: "Neptune World Series 2023",
        gamblerId: "YF23H1",
        gamblerUsername: "neptune-player",
        position: 4,
        beforePosition: 3,
        score: 8
    )
}

public func poolGamblerScoreDummyModelWithoutPositionData() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: "P6Z8V4",
        poolName: "Athena Olympics 2024",
        gamblerId: "M9L2S7",
        gamblerUsername: "athena-challenger",
        position: nil,
        beforePosition: nil,
        score: 10
    )
}

public func poolGamblerScoresDummyModels() -> [PoolGamblerScoreModel] {
    return [
        PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            position: 4,
            beforePosition: 3,
            score: 8
        ),
        PoolGamblerScoreModel(
            poolId: "X4D7B9",
            poolName: "Ares Euro Championship 2024",
            gamblerId: "ZT94Q5",
            gamblerUsername: "ares-bettor",
            position: 2,
            beforePosition: nil,
            score: 10
        ),
        PoolGamblerScoreModel(
            poolId: "J1K5M7",
            poolName: "Hermes International Cup 2023",
            gamblerId: "XN28J4",
            gamblerUsername: "hermes-punter",
            position: 1,
            beforePosition: 4,
            score: 6
        )
    ]
}
