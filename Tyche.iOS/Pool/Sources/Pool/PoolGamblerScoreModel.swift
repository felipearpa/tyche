public struct PoolGamblerScoreModelId: Hashable, Codable {
    public let poolId: String
    public let gamblerId: String
}

public struct PoolGamblerScoreModel: Identifiable, Hashable, Codable {
    public let poolId: String
    public let poolName: String
    public let gamblerId: String
    public let gamblerUsername: String
    public let currentPosition: Int?
    public let beforePosition: Int?
    public let score: Int?

    public var id: PoolGamblerScoreModelId {
        return PoolGamblerScoreModelId(poolId: poolId, gamblerId: gamblerId)
    }
}

public extension PoolGamblerScoreModel {
    func difference() -> Int? {
        guard let currentPosition = self.currentPosition else {
            return nil
        }
        
        guard let beforePosition = self.beforePosition else {
            return nil
        }
        
        return beforePosition - currentPosition
    }
}
