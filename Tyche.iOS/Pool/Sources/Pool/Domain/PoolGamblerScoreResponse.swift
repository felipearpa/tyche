struct PoolGamblerScoreResponse : Codable {
    let poolId: String
    let poolName: String
    let gamblerId: String
    let gamblerUsername: String
    let currentPosition: Int?
    let beforePosition: Int?
    let score: Int?
}

extension PoolGamblerScoreResponse {
    func toPoolGamblerScore() -> PoolGamblerScore {
        return PoolGamblerScore(
            poolId: self.poolId,
            poolName: self.poolName,
            gamblerId: self.gamblerId,
            gamblerUsername: self.gamblerUsername,
            currentPosition: self.currentPosition,
            beforePosition: self.beforePosition,
            score: self.score
        )
    }
}
