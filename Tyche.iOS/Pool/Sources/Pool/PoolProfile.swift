public struct PoolProfile : Codable {
    public let poolId: String
    
    public init(poolId: String) {
        self.poolId = poolId
    }
}
