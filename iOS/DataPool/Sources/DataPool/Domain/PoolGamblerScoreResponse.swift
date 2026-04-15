struct PoolGamblerScoreResponse : Codable {
    let poolId: String
    let poolName: String
    let gamblerId: String
    let gamblerUsername: String
    let position: Int?
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
            position: self.position,
            beforePosition: self.beforePosition,
            score: self.score
        )
    }
}
