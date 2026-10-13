import Foundation

struct PoolGamblerScoreResponse : Codable {
    let poolId: String
    let poolName: String
    let gamblerId: String
    let gamblerUsername: String
    let position: Int?
    let beforePosition: Int?
    let score: Int?
    let gamblerCount: Int?
    /// JSON null, or absent from an older server, while the pool has no settled kickoff group.
    let trendGroup: TrendGroupResponse?
}

struct TrendGroupResponse: Codable {
    let kickoffDateTime: Date
    let computedMatches: Int
    let totalMatches: Int
}
