import Core
import DataBet
import Foundation

/// One kickoff group of History or Timeline, ready to present: the server's kickoff instant,
/// counts, and movement, with its match rows in the server's order. Nothing here is recounted or
/// derived from the loaded rows.
struct KickoffGroupModel: Codable, Identifiable, Hashable, Sendable {
    /// The group's identity: its kickoff instant. A feed returns each group once.
    let kickoffDateTime: Date
    let totalMatches: Int
    let computedMatches: Int
    let movement: RankMovement
    let matches: [PoolGamblerBetModel]

    var id: Date { kickoffDateTime }
}

extension KickoffGroup {
    func toKickoffGroupModel() -> KickoffGroupModel {
        KickoffGroupModel(
            kickoffDateTime: kickoffDateTime,
            totalMatches: totalMatches,
            computedMatches: computedMatches,
            movement: .kickoffGroup(
                beforePosition: movement?.beforePosition,
                position: movement?.position,
                progress: KickoffGroupProgress(computedMatches: computedMatches, totalMatches: totalMatches),
                hasComputedMatch: matches.contains(where: \.isComputed)
            ),
            matches: matches.map { $0.toPoolGamblerBetModel() }
        )
    }
}
