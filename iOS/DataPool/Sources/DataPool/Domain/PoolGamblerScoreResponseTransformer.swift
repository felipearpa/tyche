extension PoolGamblerScoreResponse {
    func toPoolGamblerScore() -> PoolGamblerScore {
        return PoolGamblerScore(
            poolId: poolId,
            poolName: poolName,
            gamblerId: gamblerId,
            gamblerUsername: gamblerUsername,
            position: position,
            beforePosition: beforePosition,
            score: score,
            gamblerCount: gamblerCount
        )
    }
}
