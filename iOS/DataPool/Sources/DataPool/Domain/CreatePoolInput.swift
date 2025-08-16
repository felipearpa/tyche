public struct CreatePoolInput {
    public let poolLayoutId: String
    public let poolName: String
    public let ownerGamblerId: String

    public init(poolLayoutId: String, poolName: String, ownerGamblerId: String) {
        self.poolLayoutId = poolLayoutId
        self.poolName = poolName
        self.ownerGamblerId = ownerGamblerId
    }
}
