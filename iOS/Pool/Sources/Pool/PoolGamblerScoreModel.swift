public struct PoolGamblerScoreModelId: Hashable, Codable, Sendable {
    public let poolId: String
    public let gamblerId: String
}

public struct PoolGamblerScoreModel: Identifiable, Hashable, Codable, Sendable {
    public let poolId: String
    public let poolName: String
    public let gamblerId: String
    public let gamblerUsername: String
    public let position: Int?
    public let beforePosition: Int?
    public let score: Int?
    public let gamblerCount: Int?

    public var id: PoolGamblerScoreModelId {
        return PoolGamblerScoreModelId(poolId: poolId, gamblerId: gamblerId)
    }
}

public extension PoolGamblerScoreModel {
    func rank() -> Int? {
        guard let currentPosition = self.position else {
            return nil
        }
        
        guard let beforePosition = self.beforePosition else {
            return nil
        }
        
        return beforePosition - currentPosition
    }
}
