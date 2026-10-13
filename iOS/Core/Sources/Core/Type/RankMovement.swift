/// How many of a kickoff group's matches have a result. Both counts come from the pool's layout
/// on the server, never from the rows a client has loaded.
public struct KickoffGroupProgress: Codable, Hashable, Sendable {
    public let computedMatches: Int
    public let totalMatches: Int

    public init(computedMatches: Int, totalMatches: Int) {
        self.computedMatches = computedMatches
        self.totalMatches = totalMatches
    }

    /// True while some of the group's matches still lack a result.
    public var isPartial: Bool {
        computedMatches < totalMatches
    }
}

/// A gambler's rank movement across one kickoff group, as every movement surface shows it: the
/// leaderboard, My pools, History, and Timeline.
public enum RankMovement: Codable, Hashable, Sendable {
    /// No settled match yet: the pool has no trend group, or the feed group has no computed match
    /// for this gambler. Never shown as a number or as steady.
    case pending
    /// The group has a settled match but a position snapshot is missing. Never shown as zero.
    case unavailable
    /// `beforePosition - position`: positive is a gain, negative a loss, zero steady.
    /// `partialProgress` is set while the group still has matches without a result.
    case moved(places: Int, partialProgress: KickoffGroupProgress?)

    /// The leaderboard and My pools movement: the pool's trend group with the row's current and
    /// baseline positions. No trend group is pending; a trend group with either position missing
    /// is unavailable.
    public static func trendGroup(
        _ progress: KickoffGroupProgress?,
        beforePosition: Int?,
        position: Int?
    ) -> RankMovement {
        guard let progress else { return .pending }
        guard let beforePosition, let position else { return .unavailable }
        return moved(beforePosition: beforePosition, position: position, progress: progress)
    }

    /// A History or Timeline group's movement from the server's snapshots. A missing movement is
    /// pending when the gambler has no computed match in the group, and unavailable otherwise.
    public static func kickoffGroup(
        beforePosition: Int?,
        position: Int?,
        progress: KickoffGroupProgress,
        hasComputedMatch: Bool
    ) -> RankMovement {
        guard let beforePosition, let position else {
            return hasComputedMatch ? .unavailable : .pending
        }
        return moved(beforePosition: beforePosition, position: position, progress: progress)
    }

    private static func moved(beforePosition: Int, position: Int, progress: KickoffGroupProgress) -> RankMovement {
        .moved(places: beforePosition - position, partialProgress: progress.isPartial ? progress : nil)
    }
}
