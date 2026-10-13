import Foundation

/// A grouped History or Timeline item as the server sends it. `movement` is JSON null when the
/// server has no movement for the group.
struct KickoffGroupResponse: Codable {
    let kickoffDateTime: Date
    let totalMatches: Int
    let computedMatches: Int
    let movement: GroupMovementResponse?
    let matches: [PoolGamblerBetResponse]
}

struct GroupMovementResponse: Codable {
    let beforePosition: Int
    let position: Int
}

extension KickoffGroupResponse {
    func toKickoffGroup() -> KickoffGroup {
        KickoffGroup(
            kickoffDateTime: kickoffDateTime,
            totalMatches: totalMatches,
            computedMatches: computedMatches,
            movement: movement.map { GroupMovement(beforePosition: $0.beforePosition, position: $0.position) },
            matches: matches.map { $0.toPoolGamblerBet() }
        )
    }
}
