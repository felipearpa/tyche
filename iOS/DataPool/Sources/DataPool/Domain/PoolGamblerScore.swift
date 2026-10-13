import Foundation

public struct PoolGamblerScore: Codable {
    public let poolId: String
    public let poolName: String
    public let gamblerId: String
    public let gamblerUsername: String
    public let position: Int?
    /// The position before the pool's trend group first settled.
    public let beforePosition: Int?
    public let score: Int?
    public let gamblerCount: Int?
    /// The pool's newest kickoff group with a settled match; `nil` while none has settled.
    public let trendGroup: TrendGroup?
}

/// The kickoff group whose baseline `beforePosition` holds: its kickoff instant in UTC and how many
/// of its matches have a result. The leaderboard and My pools receive the same value for a pool.
public struct TrendGroup: Codable, Hashable {
    public let kickoffDateTime: Date
    public let computedMatches: Int
    public let totalMatches: Int
}
