import Core
import Foundation
import Testing
@testable import Bet

/// The spoken text of the shared kickoff-group header and of a row focused directly. Run in the
/// package's development language (English).
@MainActor
struct KickoffGroupPresentationTests {
    @Test
    func aHeaderAnnouncesDateTimeTotalCountAndMovementOnce() {
        let header = KickoffGroupHeader(group: group(total: 2, computed: 2, movement: .moved(places: 3, partialProgress: nil)), dateFormat: fixedFormat)

        #expect(header.accessibilityLabel == "Wednesday, Jul 15, 2:00\u{202F}PM. 2 matches. Up 3 places")
    }

    @Test
    func aPartialHeaderAddsTheComputedCountAfterTheMovement() {
        let progress = KickoffGroupProgress(computedMatches: 2, totalMatches: 4)
        let header = KickoffGroupHeader(
            group: group(total: 4, computed: 2, movement: .moved(places: 4, partialProgress: progress)),
            dateFormat: fixedFormat
        )

        #expect(
            header.accessibilityLabel
                == "Wednesday, Jul 15, 2:00\u{202F}PM. 4 matches. Up 4 places, 2 of 4 matches computed"
        )
    }

    @Test
    func singularCountsAndPlacesUseSingularWords() {
        let header = KickoffGroupHeader(
            group: group(total: 1, computed: 1, movement: .moved(places: -1, partialProgress: nil)),
            dateFormat: fixedFormat
        )
        let partial = KickoffGroupHeader(
            group: group(
                total: 2,
                computed: 1,
                movement: .moved(places: 1, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2))
            ),
            dateFormat: fixedFormat
        )

        #expect(header.accessibilityLabel == "Wednesday, Jul 15, 2:00\u{202F}PM. 1 match. Down 1 place")
        #expect(partial.accessibilityLabel.hasSuffix(". Up 1 place, 1 of 2 matches computed"))
    }

    @Test
    func pendingUnavailableAndSteadyAreDistinct() {
        let pending = KickoffGroupHeader(group: group(total: 2, computed: 0, movement: .pending), dateFormat: fixedFormat)
        let unavailable = KickoffGroupHeader(group: group(total: 2, computed: 2, movement: .unavailable), dateFormat: fixedFormat)
        let steady = KickoffGroupHeader(
            group: group(total: 2, computed: 2, movement: .moved(places: 0, partialProgress: nil)),
            dateFormat: fixedFormat
        )

        #expect(pending.accessibilityLabel.hasSuffix(". 2 matches. Movement pending"))
        #expect(unavailable.accessibilityLabel.hasSuffix(". 2 matches. Movement unavailable"))
        #expect(steady.accessibilityLabel.hasSuffix(". 2 matches. Rank unchanged"))
    }

    @Test
    func aPlaceholderHeaderAnnouncesNothing() {
        #expect(KickoffGroupHeader(group: kickoffGroupPlaceholderModel(), isPlaceholder: true).accessibilityLabel.isEmpty)
    }

    @Test
    func aRowFocusedDirectlyKeepsItsOwnDateAndTimeWithoutTheGroupCountOrMovement() {
        let progress = KickoffGroupProgress(computedMatches: 1, totalMatches: 2)
        let model = group(total: 2, computed: 1, movement: .moved(places: 3, partialProgress: progress))
        let row = HistoryBetItem(poolGamblerBet: model.matches[0], dateFormat: fixedFormat).accessibilityLabel

        #expect(row.hasPrefix("Wednesday, Jul 15, 2:00\u{202F}PM. "))
        #expect(!row.contains("matches"))
        #expect(!row.contains("place"))
    }

    @Test
    func timelineRowsUseNeutralWordingUnderTheSharedHeader() {
        let model = group(total: 2, computed: 2, movement: .moved(places: 3, partialProgress: nil))
        let history = HistoryBetItem(poolGamblerBet: model.matches[0], dateFormat: fixedFormat).accessibilityLabel
        let timeline = HistoryBetItem(
            poolGamblerBet: model.matches[0],
            owner: .selectedGambler(name: "El mono"),
            dateFormat: fixedFormat
        )
        .accessibilityLabel

        #expect(history.contains(". Your bet: "))
        #expect(timeline.contains(". Bet: "))
        #expect(!timeline.contains("Your"))
    }

    @Test
    func theGroupModelTakesCountsAndMovementFromTheServerNotTheRows() throws {
        let partial = GroupFixtures.group("m", total: 4, computed: 2, rows: 1, movement: (8, 6)).toKickoffGroupModel()
        let legacy = GroupFixtures.group("l", total: 1, computed: 1, movement: nil).toKickoffGroupModel()
        let unsettled = GroupFixtures.group("u", total: 2, computed: 1, computedRows: 0, movement: nil).toKickoffGroupModel()

        #expect(partial.totalMatches == 4)
        #expect(partial.computedMatches == 2)
        #expect(partial.matches.count == 1)
        #expect(partial.movement == .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 2, totalMatches: 4)))
        #expect(legacy.movement == .unavailable)
        // The layout already has a result, but the gambler's row is not computed yet.
        #expect(unsettled.movement == .pending)
    }

    // MARK: Fixtures

    private let utc = TimeZone(identifier: "UTC")!

    private var fixedFormat: HistoryMatchDateFormat {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = utc
        return HistoryMatchDateFormat(locale: Locale(identifier: "en_US"), calendar: calendar, timeZone: utc)
    }

    /// Wednesday, 15 July 2026, 14:00 UTC.
    private let kickoff = Date(timeIntervalSince1970: 1_784_124_000)

    private func group(total: Int, computed: Int, movement: RankMovement) -> KickoffGroupModel {
        KickoffGroupModel(
            kickoffDateTime: kickoff,
            totalMatches: total,
            computedMatches: computed,
            movement: movement,
            matches: (0..<total).map { index in
                poolGamblerBetDummyModel().copy {
                    $0.matchId = "match-\(index)"
                    $0.matchDateTime = kickoff
                    $0.isComputed = index < computed
                }
            }
        )
    }
}
