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
            gamblerCount: gamblerCount,
            trendGroup: trendGroup.map {
                TrendGroup(
                    kickoffDateTime: $0.kickoffDateTime,
                    computedMatches: $0.computedMatches,
                    totalMatches: $0.totalMatches
                )
            }
        )
    }
}
