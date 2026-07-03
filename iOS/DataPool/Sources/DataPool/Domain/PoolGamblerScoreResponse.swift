struct PoolGamblerScoreResponse : Codable {
    let poolId: String
    let poolName: String
    let gamblerId: String
    let gamblerUsername: String
    let position: Int?
    let beforePosition: Int?
    let score: Int?
    let gamblerCount: Int?
}
