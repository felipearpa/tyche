public struct JoinPoolInput {
    let poolId: String
    let gamblerId: String

    public init(poolId: String, gamblerId: String) {
        self.poolId = poolId
        self.gamblerId = gamblerId
    }
}
