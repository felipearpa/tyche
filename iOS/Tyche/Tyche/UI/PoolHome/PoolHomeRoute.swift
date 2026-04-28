public struct PoolHomeRoute: Hashable {
    public let poolId: String
    public let gamblerId: String

    public init(poolId: String, gamblerId: String) {
        self.poolId = poolId
        self.gamblerId = gamblerId
    }
}
