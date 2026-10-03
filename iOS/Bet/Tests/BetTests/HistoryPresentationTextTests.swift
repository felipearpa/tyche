import Core
import Foundation
import Testing
@testable import Bet

/// The visible and spoken text of History's summary and rows. Run in the package's development
/// language (English).
@MainActor
struct HistoryPresentationTextTests {
    // MARK: Points

    @Test
    func positivePointsAreSignedAndZeroOrUnavailableNeverShowAPlus() {
        #expect(historyPointsShortText(.earned(2)) == "+2 pts")
        #expect(historyPointsShortText(.earned(1)) == "+1 pt")
        #expect(historyPointsShortText(.earned(0)) == "0 pts")
        #expect(historyPointsShortText(.unavailable) == "— pts")
    }

    @Test
    func theSummaryAnnouncesCurrentPoolPointsWithSingularAndPluralWords() {
        #expect(HistoryPointsSummary(points: .earned(1)).accessibilityLabel == "1 point earned in this pool")
        #expect(HistoryPointsSummary(points: .earned(120)).accessibilityLabel == "120 points earned in this pool")
        #expect(HistoryPointsSummary(points: .earned(0)).accessibilityLabel == "0 points earned in this pool")
        #expect(
            HistoryPointsSummary(points: .unavailable).accessibilityLabel
                == "Points earned in this pool unavailable"
        )
    }

    @Test
    func placeholdersAnnounceNothing() {
        #expect(HistoryPointsSummary(points: historyPointsPlaceholderModel, isPlaceholder: true).accessibilityLabel.isEmpty)
        #expect(HistoryBetItem(poolGamblerBet: historyBetPlaceholderModel(), isPlaceholder: true).accessibilityLabel.isEmpty)
    }

    // MARK: Rows

    @Test
    func aRowAnnouncesDateResultBetAndPointsInOrder() {
        let bet = row(match: TeamScore(homeTeamValue: 1, awayTeamValue: 2), bet: TeamScore(homeTeamValue: 2, awayTeamValue: 1), points: 1)

        #expect(
            HistoryBetItem(poolGamblerBet: bet, dateFormat: fixedFormat).accessibilityLabel
                == "Wednesday, Jul 15, 2:00\u{202F}PM. Final score: Inglaterra 1, Argentina 2. Your bet: 2 to 1. 1 point"
        )
    }

    @Test
    func unavailableValuesAreDescribedRatherThanReadAsZero() {
        let bet = row(match: nil, bet: nil, points: nil)

        #expect(
            HistoryBetItem(poolGamblerBet: bet, dateFormat: fixedFormat).accessibilityLabel
                == "Wednesday, Jul 15, 2:00\u{202F}PM. Inglaterra versus Argentina, final score unavailable. No bet placed. Points unavailable"
        )
    }

    // MARK: Date and time

    @Test
    func aMatchInTheCurrentYearOmitsTheYear() {
        #expect(fixedFormat.date(matchDate, now: matchDate) == "Wednesday, Jul 15")
    }

    @Test
    func aMatchFromAnotherYearIncludesIt() throws {
        let nextYear = try #require(utcCalendar.date(byAdding: .year, value: 1, to: matchDate))

        #expect(fixedFormat.date(matchDate, now: nextYear) == "Wednesday, Jul 15, 2026")
    }

    @Test
    func theDateFollowsTheLocaleOrderAndLanguage() {
        let spanish = HistoryMatchDateFormat(locale: Locale(identifier: "es_CO"), calendar: utcCalendar, timeZone: utc)

        #expect(spanish.date(matchDate, now: matchDate) == "miércoles, 15 de jul.")
    }

    @Test
    func theTimeFollowsTheClockPreference() {
        let twentyFourHour = HistoryMatchDateFormat(
            locale: Locale(identifier: "en_US@hours=h23"),
            calendar: utcCalendar,
            timeZone: utc
        )

        // The locale separates the time from its period with a narrow no-break space.
        #expect(fixedFormat.time(matchDate) == "2:00\u{202F}PM")
        #expect(twentyFourHour.time(matchDate) == "14:00")
    }

    // MARK: Fixtures

    private let utc = TimeZone(identifier: "UTC")!

    private var utcCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = utc
        return calendar
    }

    private var fixedFormat: HistoryMatchDateFormat {
        HistoryMatchDateFormat(locale: Locale(identifier: "en_US"), calendar: utcCalendar, timeZone: utc)
    }

    /// Wednesday, 15 July 2026, 14:00 UTC.
    private var matchDate: Date {
        Date(timeIntervalSince1970: 1_784_124_000)
    }

    private func row(match: TeamScore<Int>?, bet: TeamScore<Int>?, points: Int?) -> PoolGamblerBetModel {
        poolGamblerBetDummyModel().copy {
            $0.homeTeamName = "Inglaterra"
            $0.awayTeamName = "Argentina"
            $0.matchScore = match
            $0.betScore = bet
            $0.score = points
            $0.matchDateTime = matchDate
        }
    }
}
