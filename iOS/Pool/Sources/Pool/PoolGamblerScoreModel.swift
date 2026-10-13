import Core

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
    /// The position before the pool's trend group first settled.
    public let beforePosition: Int?
    public let score: Int?
    public let gamblerCount: Int?
    /// The computed and total counts of the pool's trend group; `nil` while no kickoff group of
    /// the pool has settled.
    public let trendGroup: KickoffGroupProgress?

    public var id: PoolGamblerScoreModelId {
        return PoolGamblerScoreModelId(poolId: poolId, gamblerId: gamblerId)
    }

    public init(
        poolId: String,
        poolName: String,
        gamblerId: String,
        gamblerUsername: String,
        position: Int?,
        beforePosition: Int?,
        score: Int?,
        gamblerCount: Int?,
        trendGroup: KickoffGroupProgress?
    ) {
        self.poolId = poolId
        self.poolName = poolName
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.position = position
        self.beforePosition = beforePosition
        self.score = score
        self.gamblerCount = gamblerCount
        self.trendGroup = trendGroup
    }
}

public extension PoolGamblerScoreModel {
    /// The trend group's movement, shared by the leaderboard and My pools: pending without a
    /// trend group, unavailable when either position is missing, and otherwise
    /// `beforePosition - position` with the group's progress while it is partial.
    var movement: RankMovement {
        .trendGroup(trendGroup, beforePosition: beforePosition, position: position)
    }
}
