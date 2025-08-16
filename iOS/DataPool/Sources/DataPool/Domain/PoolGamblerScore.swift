public struct PoolGamblerScore: Codable {
    public let poolId: String
    public let poolName: String
    public let gamblerId: String
    public let gamblerUsername: String
    public let currentPosition: Int?
    public let beforePosition: Int?
    public let score: Int?
}
