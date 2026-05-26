struct PoolResponse: Codable {
    let poolId: String
    let poolName: String
    let creatorGamblerId: String
    let gamblerCount: Int?
}
