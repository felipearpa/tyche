public struct GamblerBetsRoute: Hashable {
    public let poolId: String
    public let gamblerId: String
    public let gamblerUsername: String

    public init(poolId: String, gamblerId: String, gamblerUsername: String) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
    }
}
