import Foundation

/// One item of the grouped History and Timeline feeds: the matches of a pool that share one
/// scheduled kickoff instant. The server sends whole groups, so the counts and movement describe
/// the complete group whatever page a client has loaded.
public struct KickoffGroup: Codable {
    /// The group's identity: its kickoff instant in UTC.
    public let kickoffDateTime: Date
    /// The layout's match count at this kickoff, independent of the rows visible to the caller.
    public let totalMatches: Int
    /// How many of those matches have a result.
    public let computedMatches: Int
    /// The gambler's positions before and after the group, from stored snapshots. `nil` when
    /// the gambler has no computed match in the group or a snapshot is missing.
    public let movement: GroupMovement?
    /// The group's visible matches, ordered by match identifier.
    public let matches: [PoolGamblerBet]
}

/// A gambler's position before a kickoff group's first settlement and after its latest one.
public struct GroupMovement: Codable, Hashable {
    public let beforePosition: Int
    public let position: Int
}
