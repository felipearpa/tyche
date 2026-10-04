import Core
import Foundation
import Testing
@testable import Bet

/// The spoken text of History's shared row and summary when they describe another gambler on
/// Timeline, and for entries whose points are not computed yet. Personal History's defaults are
/// covered by `HistoryPresentationTextTests`. Run in the package's development language
/// (English) unless a test names a locale.
@MainActor
struct TimelinePresentationTextTests {
    private let selected = HistoryOwner.selectedGambler(name: "El mono")

    // MARK: Settled rows

    @Test
    func aSettledRowAnnouncesTheSelectedGamblersBetWithNeutralWording() {
        let bet = row(match: score(1, 2), bet: score(2, 1), points: 2)

        #expect(
            label(bet, owner: selected)
                == "Wednesday, Jul 15, 2:00\u{202F}PM. Final score: Inglaterra 1, Argentina 2. Bet: 2 to 1. 2 points"
        )
    }

    @Test
    func aSingleAndAZeroAwardUseFullPointWords() {
        #expect(label(row(match: score(1, 1), bet: score(1, 1), points: 1), owner: selected).hasSuffix(". 1 point"))
        #expect(label(row(match: score(0, 0), bet: score(1, 2), points: 0), owner: selected).hasSuffix(". 0 points"))
    }

    @Test
    func missingValuesAreDescribedWithoutAddressingTheViewer() {
        let bet = row(match: nil, bet: nil, points: nil)

        #expect(
            label(bet, owner: selected)
                == "Wednesday, Jul 15, 2:00\u{202F}PM. Inglaterra versus Argentina, final score unavailable. No bet placed. Points unavailable"
        )
    }

    @Test
    func personalHistoryKeepsItsOwnershipWordingByDefault() {
        let bet = row(match: score(1, 2), bet: score(2, 1), points: 2)

        #expect(label(bet, owner: .signedInGambler).contains(". Your bet: 2 to 1. "))
        #expect(HistoryBetItem(poolGamblerBet: bet, dateFormat: fixedFormat).accessibilityLabel == label(bet, owner: .signedInGambler))
    }

    // MARK: Pending rows

    @Test
    func aPendingRowWithAScoreDescribesItAsTheMatchScoreWithPendingPoints() {
        let bet = row(match: score(1, 0), bet: score(2, 1), points: nil, computed: false)

        #expect(
            label(bet, owner: selected)
                == "Wednesday, Jul 15, 2:00\u{202F}PM. Match score: Inglaterra 1, Argentina 0. Bet: 2 to 1. Points pending"
        )
    }

    @Test
    func aPendingRowWithoutAScoreSaysTheScoreIsUnavailable() {
        let bet = row(match: nil, bet: nil, points: nil, computed: false)

        #expect(
            label(bet, owner: selected)
                == "Wednesday, Jul 15, 2:00\u{202F}PM. Inglaterra versus Argentina, match score unavailable. No bet placed. Points pending"
        )
    }

    @Test
    func aPendingEntryNeverShowsAValueAsAnAward() {
        let bet = row(match: score(1, 0), bet: score(1, 0), points: 5, computed: false)

        #expect(HistoryRowPoints(bet) == .pending)
        #expect(!label(bet, owner: selected).contains("5 points"))
        #expect(HistoryRowPoints(row(match: score(1, 0), bet: nil, points: 0)) == .awarded(.earned(0)))
        #expect(HistoryRowPoints(row(match: nil, bet: nil, points: nil)) == .awarded(.unavailable))
    }

    // MARK: Summary

    @Test
    func theSummaryNamesTheSelectedGamblerWithSingularAndPluralWords() {
        #expect(HistoryPointsSummary(points: .earned(1), owner: selected).accessibilityLabel == "El mono earned 1 point in this pool")
        #expect(HistoryPointsSummary(points: .earned(120), owner: selected).accessibilityLabel == "El mono earned 120 points in this pool")
        #expect(HistoryPointsSummary(points: .earned(0), owner: selected).accessibilityLabel == "El mono earned 0 points in this pool")
        #expect(
            HistoryPointsSummary(points: .unavailable, owner: selected).accessibilityLabel
                == "Points earned by El mono in this pool unavailable"
        )
    }

    @Test
    func placeholdersAnnounceNothingForTheSelectedGambler() {
        #expect(HistoryPointsSummary(points: historyPointsPlaceholderModel, owner: selected, isPlaceholder: true).accessibilityLabel.isEmpty)
        #expect(HistoryBetItem(poolGamblerBet: historyBetPlaceholderModel(), isPlaceholder: true, owner: selected).accessibilityLabel.isEmpty)
    }

    // MARK: Spanish

    @Test
    func spanishSelectedGamblerTextUsesThirdPersonAndSingularForms() {
        #expect(spanish(.historyGamblerNoBetAccessibility) == "No hay apuesta registrada")
        #expect(spanish(.historyGamblerBetAccessibility(2, 1)) == "Apuesta: 2 a 1")
        #expect(spanish(.historyGamblerSummaryPointsAccessibility("El mono", points: 1)) == "El mono ganó 1 punto en esta polla")
        #expect(spanish(.historyGamblerSummaryPointsAccessibility("El mono", points: 3)) == "El mono ganó 3 puntos en esta polla")
        #expect(spanish(.historyPointsPendingLabel) == "Puntos pendientes")
        #expect(
            spanish(.historyGamblerSummaryPointsAccessibility("El mono", points: 1), identifier: "es-ES")
                == "El mono ganó 1 punto en esta quiniela"
        )
    }

    // MARK: Fixtures

    private func spanish(_ resource: LocalizedStringResource, identifier: String = "es") -> String {
        var resource = resource
        resource.locale = Locale(identifier: identifier)
        return String(localized: resource)
    }

    private func label(_ bet: PoolGamblerBetModel, owner: HistoryOwner) -> String {
        HistoryBetItem(poolGamblerBet: bet, owner: owner, dateFormat: fixedFormat).accessibilityLabel
    }

    private let utc = TimeZone(identifier: "UTC")!

    private var fixedFormat: HistoryMatchDateFormat {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = utc
        return HistoryMatchDateFormat(locale: Locale(identifier: "en_US"), calendar: calendar, timeZone: utc)
    }

    /// Wednesday, 15 July 2026, 14:00 UTC.
    private var matchDate: Date {
        Date(timeIntervalSince1970: 1_784_124_000)
    }

    private func score(_ home: Int, _ away: Int) -> TeamScore<Int> {
        TeamScore(homeTeamValue: home, awayTeamValue: away)
    }

    private func row(
        match: TeamScore<Int>?,
        bet: TeamScore<Int>?,
        points: Int?,
        computed: Bool = true
    ) -> PoolGamblerBetModel {
        poolGamblerBetDummyModel().copy {
            $0.homeTeamName = "Inglaterra"
            $0.awayTeamName = "Argentina"
            $0.matchScore = match
            $0.betScore = bet
            $0.score = points
            $0.matchDateTime = matchDate
            $0.isComputed = computed
        }
    }
}
