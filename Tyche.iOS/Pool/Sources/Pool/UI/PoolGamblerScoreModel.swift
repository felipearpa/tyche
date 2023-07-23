struct PoolGamblerScoreModelId: Hashable, Codable {
    let poolId: String
    let gamblerId: String
}

struct PoolGamblerScoreModel: Identifiable, Hashable, Codable {
    let poolId: String
    let poolName: String
    let gamblerId: String
    let gamblerUsername: String
    let currentPosition: Int?
    let beforePosition: Int?
    let score: Int?
    
    var id: PoolGamblerScoreModelId {
        return PoolGamblerScoreModelId(poolId: poolId, gamblerId: gamblerId)
    }
}

extension PoolGamblerScoreModel {
    func calculateDifference() -> Int? {
        guard let currentPosition = self.currentPosition else {
            return nil
        }
        
        guard let beforePosition = self.beforePosition else {
            return nil
        }
        
        return beforePosition - currentPosition
    }
}
