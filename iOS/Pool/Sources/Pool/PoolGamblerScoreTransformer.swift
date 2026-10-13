import Core
import DataPool

public extension PoolGamblerScore {
    func toPoolGamblerScoreModel() -> PoolGamblerScoreModel {
        PoolGamblerScoreModel(
            poolId: poolId,
            poolName: poolName,
            gamblerId: gamblerId,
            gamblerUsername: gamblerUsername,
            position: position,
            beforePosition: beforePosition,
            score: score,
            gamblerCount: gamblerCount,
            trendGroup: trendGroup.map {
                KickoffGroupProgress(computedMatches: $0.computedMatches, totalMatches: $0.totalMatches)
            }
        )
    }
}
