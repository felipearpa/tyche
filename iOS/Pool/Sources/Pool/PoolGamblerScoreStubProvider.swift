import Core
import Foundation

/// A settled two-match trend group.
private let completeTrendGroup = KickoffGroupProgress(computedMatches: 2, totalMatches: 2)

public func poolGamblerScorePlaceholderModel() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: UUID().uuidString,
        poolName: String(repeating: "X", count: 25),
        gamblerId: UUID().uuidString,
        gamblerUsername: String(repeating: "X", count: 15),
        position: 1,
        beforePosition: 2,
        score: 10,
        gamblerCount: 71,
        // A numeric movement, the rank rail's usual content between kickoffs.
        trendGroup: completeTrendGroup
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
        score: 8,
        gamblerCount: 101,
        trendGroup: completeTrendGroup
    )
}

public func poolGamblerScoreWithoutPositionDummyModel() -> PoolGamblerScoreModel {
    return PoolGamblerScoreModel(
        poolId: "A3C2E1",
        poolName: "Neptune World Series 2023",
        gamblerId: "YF23H1",
        gamblerUsername: "neptune-player",
        position: nil,
        beforePosition: nil,
        score: 0,
        gamblerCount: 69,
        trendGroup: completeTrendGroup
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
        score: 10,
        gamblerCount: 32,
        trendGroup: completeTrendGroup
    )
}

public func poolGamblerScoresDummyModels() -> [PoolGamblerScoreModel] {
    return [
        PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            position: nil,
            beforePosition: nil,
            score: 0,
            gamblerCount: 67,
            trendGroup: completeTrendGroup
        ),
        PoolGamblerScoreModel(
            poolId: "X4D7B9",
            poolName: "Ares Euro Championship 2024",
            gamblerId: "ZT94Q5",
            gamblerUsername: "ares-bettor",
            position: 2,
            beforePosition: 1,
            score: 10,
            gamblerCount: 111,
            trendGroup: completeTrendGroup
        ),
        PoolGamblerScoreModel(
            poolId: "J1K5M7",
            poolName: "Hermes International Cup 2023",
            gamblerId: "XN28J4",
            gamblerUsername: "hermes-punter",
            position: 1,
            beforePosition: 4,
            score: 6,
            gamblerCount: 43,
            trendGroup: completeTrendGroup
        )
    ]
}

/// One row per trend-group movement state: complete gain, partial gain, partial loss, steady,
/// pending (no trend group), and unavailable (missing baseline). Preview data only.
public func poolGamblerScoreTrendStateDummyModels() -> [PoolGamblerScoreModel] {
    let partial = KickoffGroupProgress(computedMatches: 1, totalMatches: 2)
    let states: [(String, Int?, Int?, KickoffGroupProgress?)] = [
        ("trend-complete", 3, 6, completeTrendGroup),
        ("trend-partial", 5, 8, partial),
        ("trend-partial-loss", 9, 7, KickoffGroupProgress(computedMatches: 2, totalMatches: 4)),
        ("trend-steady", 4, 4, completeTrendGroup),
        ("trend-pending", 2, 2, nil),
        ("trend-unavailable", 12, nil, completeTrendGroup),
    ]
    return states.enumerated().map { index, state in
        PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: state.0,
            gamblerUsername: "gambler-\(index + 1)",
            position: state.1,
            beforePosition: state.2,
            score: 120 - index * 10,
            gamblerCount: 101,
            trendGroup: state.3
        )
    }
}
