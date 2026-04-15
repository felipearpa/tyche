import DataPool

public extension PoolGamblerScore {
    func toPoolGamblerScoreModel() -> PoolGamblerScoreModel {
        PoolGamblerScoreModel(
            poolId: self.poolId,
            poolName: self.poolName,
            gamblerId: self.gamblerId,
            gamblerUsername: self.gamblerUsername,
            position: self.position,
            beforePosition: self.beforePosition,
            score: self.score
        )
    }
}
