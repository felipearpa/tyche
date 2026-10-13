import Core
import Foundation

/// Stable filler for a loading group: a header with a numeric movement and two finished rows, the
/// usual shape of a kickoff. Never shown as real content.
func kickoffGroupPlaceholderModel() -> KickoffGroupModel {
    KickoffGroupModel(
        kickoffDateTime: Date(timeIntervalSince1970: 0),
        totalMatches: 2,
        computedMatches: 2,
        movement: .moved(places: 1, partialProgress: nil),
        matches: [
            historyBetPlaceholderModel().copy { $0.matchId = "placeholder-1" },
            historyBetPlaceholderModel().copy { $0.matchId = "placeholder-2" },
        ]
    )
}

/// History preview groups, newest first: two matches that moved up, one that moved down, a
/// steady three-match group with long names and missing values, a partial four-match group, and
/// a group whose movement is unavailable. Preview data only.
func historyGroupPreviewModels(now: Date = Date()) -> [KickoffGroupModel] {
    let rows = historyBetPreviewModels()
    let lastYear = Calendar.current.date(byAdding: .year, value: -1, to: now) ?? now
    return [
        previewGroup(
            at: now.addingTimeInterval(-1 * 86_400),
            total: 2,
            computed: 2,
            movement: .moved(places: 3, partialProgress: nil),
            matches: [rows[1], rows[0]]
        ),
        previewGroup(
            at: now.addingTimeInterval(-2 * 86_400),
            total: 1,
            computed: 1,
            movement: .moved(places: -1, partialProgress: nil),
            matches: [rows[0].copy { $0.matchId = "history-single" }]
        ),
        previewGroup(
            at: now.addingTimeInterval(-5 * 86_400),
            total: 4,
            computed: 2,
            movement: .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 2, totalMatches: 4)),
            matches: [rows[0].copy { $0.matchId = "history-partial-1" }, rows[1].copy { $0.matchId = "history-partial-2" }]
        ),
        previewGroup(
            at: now.addingTimeInterval(-6 * 86_400),
            total: 3,
            computed: 3,
            movement: .moved(places: 0, partialProgress: nil),
            matches: [rows[2], rows[1].copy { $0.matchId = "history-steady-2" }, rows[0].copy { $0.matchId = "history-steady-3" }]
        ),
        previewGroup(
            at: lastYear,
            total: 1,
            computed: 1,
            movement: .unavailable,
            matches: [rows[3]]
        ),
    ]
}

/// Timeline preview groups: an unsettled group with no result yet (pending movement), an
/// unsettled group with one of two matches computed, then the settled History groups. Preview
/// data only.
func timelineGroupPreviewModels(now: Date = Date()) -> [KickoffGroupModel] {
    let pending = timelineBetPreviewModels()
    return [
        previewGroup(
            at: now.addingTimeInterval(-1 * 3_600),
            total: 2,
            computed: 0,
            movement: .pending,
            matches: [pending[0], pending[1]]
        ),
        previewGroup(
            at: now.addingTimeInterval(-3 * 3_600),
            total: 2,
            computed: 1,
            movement: .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2)),
            matches: [historyBetPreviewModels()[0].copy { $0.matchId = "timeline-partial-1" }, pending[0].copy { $0.matchId = "timeline-partial-2" }]
        ),
    ] + historyGroupPreviewModels(now: now)
}

/// Groups of one to four matches, for checking the rail and dividers. Preview data only.
func groupSizePreviewModels(now: Date = Date()) -> [KickoffGroupModel] {
    let rows = historyBetPreviewModels()
    return (1...4).map { size in
        previewGroup(
            at: now.addingTimeInterval(Double(-size) * 86_400),
            total: size,
            computed: size,
            movement: .moved(places: size - 2, partialProgress: nil),
            matches: (0..<size).map { index in rows[index % 2].copy { $0.matchId = "size-\(size)-\(index)" } }
        )
    }
}

private func previewGroup(
    at kickoff: Date,
    total: Int,
    computed: Int,
    movement: RankMovement,
    matches: [PoolGamblerBetModel]
) -> KickoffGroupModel {
    KickoffGroupModel(
        kickoffDateTime: kickoff,
        totalMatches: total,
        computedMatches: computed,
        movement: movement,
        matches: matches.map { match in match.copy { $0.matchDateTime = kickoff } }
    )
}
