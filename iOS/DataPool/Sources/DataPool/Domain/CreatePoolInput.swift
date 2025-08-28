public struct CreatePoolInput {
    public let poolLayoutId: String
    public let poolName: PoolName
    public let ownerGamblerId: String

    public init(poolLayoutId: String, poolName: String, ownerGamblerId: String) {
        self.poolLayoutId = poolLayoutId
        self.poolName = PoolName(poolName)!
        self.ownerGamblerId = ownerGamblerId
    }
}
