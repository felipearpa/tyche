import Core
import Foundation
import Testing
@testable import Bet

/// The match screen's state mapping and spoken text. Run in the package's development language
/// (English) unless a test names a locale.
@MainActor
struct MatchBetsPresentationTests {
    private typealias F = MatchFixtures

    // MARK: State mapping

    @Test
    func theLockAndComputationFlagsSelectThePresentation() {
        #expect(MatchBetsPresentation(match: F.match(locked: false)) == .hidden)
        #expect(MatchBetsPresentation(match: F.match(locked: true)) == .betsClosed)
        #expect(MatchBetsPresentation(match: F.match(locked: true, computed: true)) == .computed)
        // Computation takes precedence over the lock flag.
        #expect(MatchBetsPresentation(match: F.match(locked: false, computed: true)) == .computed)
    }

    @Test
    func theServersHiddenAnswerWinsOverEveryFlag() {
        let computed = F.match(locked: true, computed: true)
        #expect(MatchBetsPresentation(match: computed, predictionsHiddenByServer: true) == .hidden)
        #expect(MatchHeadingState.initial.loaded(computed).hiddenByServer().confirmedPresentation == .hidden)
        #expect(MatchHeadingState.initial.loaded(computed).hiddenByServer().loaded(computed).confirmedPresentation == .computed)
    }

    @Test
    func theCenterNeverInventsAScoreOrCallsAnUncomputedScoreFinal() {
        let zero = TeamScore(homeTeamValue: 0, awayTeamValue: 0)
        let oneNil = TeamScore(homeTeamValue: 1, awayTeamValue: 0)
        #expect(MatchHeaderCenter(match: F.match(locked: false, matchScore: oneNil), presentation: .hidden) == .versus)
        #expect(MatchHeaderCenter(match: F.match(locked: true), presentation: .betsClosed) == .versus)
        #expect(MatchHeaderCenter(match: F.match(locked: true, matchScore: oneNil), presentation: .betsClosed) == .matchScore(oneNil))
        #expect(MatchHeaderCenter(match: F.match(locked: true, computed: true, matchScore: zero), presentation: .computed) == .finalScore(zero))
        #expect(MatchHeaderCenter(match: F.match(locked: true, computed: true), presentation: .computed) == .finalScore(nil))
    }

    // MARK: Heading announcements

    @Test
    func eachHeadingStateHasItsOwnAnnouncement() {
        #expect(header(F.match(locked: false), .hidden) == "España versus Argentina. Starts Wednesday, Jul 15 · 2:00\u{202F}PM")
        #expect(header(F.match(locked: true), .betsClosed) == "España versus Argentina. Bets closed")
        #expect(
            header(F.match(locked: true, matchScore: TeamScore(homeTeamValue: 1, awayTeamValue: 0)), .betsClosed)
                == "Match score: España 1, Argentina 0. Bets closed"
        )
        #expect(
            header(F.match(locked: true, computed: true, matchScore: TeamScore(homeTeamValue: 0, awayTeamValue: 0)), .computed)
                == "Final score: España 0, Argentina 0. Wednesday, Jul 15 · 2:00\u{202F}PM"
        )
        #expect(
            header(F.match(locked: true, computed: true), .computed)
                == "España versus Argentina, final score unavailable. Wednesday, Jul 15 · 2:00\u{202F}PM"
        )
        #expect(MatchHeader(match: matchHeaderPlaceholderModel(), presentation: .hidden, isPlaceholder: true).accessibilityLabel == "")
    }

    // MARK: Participant announcements

    @Test
    func aParticipantIsAnnouncedWithATeamQualifiedPredictionAndTheMatchAward() {
        let row = F.match(gamblerId: "El mono", locked: true, computed: true, bet: TeamScore(homeTeamValue: 1, awayTeamValue: 2), award: 4)
        #expect(MatchGamblerBetItem(poolGamblerBet: row).accessibilityLabel == "El mono, Bet: España 1, Argentina 2, 4 points")
    }

    @Test
    func theSignedInParticipantIsAnnouncedAsYouWithSingularPoints() {
        let row = F.match(gamblerId: "felipearpa", locked: true, computed: true, award: 1)
        #expect(
            MatchGamblerBetItem(poolGamblerBet: row, isCurrentUser: true).accessibilityLabel
                == "felipearpa, You, Bet: España 3, Argentina 2, 1 point"
        )
    }

    @Test
    func noBetAndAnUnavailableAwardStayDistinctFromZero() {
        let unavailable = F.match(gamblerId: "Abuela", locked: true, computed: true, bet: nil, award: nil)
        let zero = F.match(gamblerId: "Abuela", locked: true, computed: true, bet: nil, award: 0)
        #expect(MatchGamblerBetItem(poolGamblerBet: unavailable).accessibilityLabel == "Abuela, No bet, Points unavailable")
        #expect(MatchGamblerBetItem(poolGamblerBet: zero).accessibilityLabel == "Abuela, No bet, 0 points")
    }

    @Test
    func anUncomputedRowAnnouncesNoAward() {
        let row = F.match(gamblerId: "Juano", locked: true, award: 4)
        #expect(MatchGamblerBetItem(poolGamblerBet: row).accessibilityLabel == "Juano, Bet: España 3, Argentina 2")
    }

    @Test
    func aPlaceholderRowIsSilentAndRequestsNoAvatar() {
        let item = MatchGamblerBetItem(poolGamblerBet: matchGamblerBetPlaceholderModel(), isPlaceholder: true)
        #expect(item.accessibilityLabel == "")
        #expect(item.avatarAccountId == "")
        #expect(MatchGamblerBetItem(poolGamblerBet: F.match(gamblerId: "g1", locked: true)).avatarAccountId == "g1")
    }

    @Test
    func theOwnPredictionIsAnnouncedAsTheViewers() {
        let match = F.match(locked: false)
        #expect(
            MatchOwnBetLine(match: match, betScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2)).accessibilityLabel
                == "Your bet: España 3, Argentina 2"
        )
        #expect(MatchOwnBetLine(match: match, betScore: nil).accessibilityLabel == "No bet placed")
        #expect(MatchOwnBetLine(match: match, betScore: matchOwnBetPlaceholderScore, isPlaceholder: true).accessibilityLabel == "")
    }

    @Test
    func longEmailNamesWrapAfterTheAtSign() {
        #expect(breakableName("juandahoyos28@gmail.com") == "juandahoyos28@\u{200B}gmail.com")
        #expect(breakableName("El mono") == "El mono")
    }

    // MARK: Entry

    @Test
    func anUpcomingMatchEntryOpensTheMatchScreen() {
        var opened: [String] = []
        let upcoming = F.match(gamblerId: "El mono", locked: false)
        #expect(upcoming.isPending)

        invokeMatchOpen({ _, gamblerId, matchId in opened.append("\(gamblerId)/\(matchId)") }, upcoming)

        #expect(opened == ["El mono/match"])
    }

    // MARK: Spanish

    @Test
    func spanishCopyFollowsTheDesign() {
        #expect(spanish(.matchBetsHiddenTitle) == "Las apuestas están ocultas")
        #expect(
            spanish(.matchBetsHiddenMessage)
                == "Las apuestas de todos se mostrarán cuando se cierren las apuestas de este partido."
        )
        #expect(spanish(.historyYourBetLabel) == "Tu apuesta")
        #expect(spanish(.historyNoBetLabel) == "Sin apuesta")
        #expect(spanish(.matchBetsClosedLabel) == "Apuestas cerradas")
        #expect(spanish(.matchBetsYouLabel) == "Tú")
    }

    @Test
    func spanishPointsUseSingularAndTeamsStayWithTheirGoals() {
        #expect(spanish(.historyPointsAccessibility(1)) == "1 punto")
        #expect(spanish(.historyPointsAccessibility(4)) == "4 puntos")
        #expect(spanish(.historyPositivePointsShort(1)) == "+1 pt")
        #expect(spanish(.matchBetsParticipantBetAccessibility("España", 1, "Argentina", 2)) == "Apuesta: España 1, Argentina 2")
        #expect(spanish(.matchBetsListChangedTitle, identifier: "es-ES") == "La clasificación ha cambiado")
    }

    // MARK: Fixtures

    private func header(_ match: PoolGamblerBetModel, _ presentation: MatchBetsPresentation) -> String {
        MatchHeader(match: match.copy { $0.matchDateTime = matchDate }, presentation: presentation, dateFormat: fixedFormat)
            .accessibilityLabel
    }

    private func spanish(_ resource: LocalizedStringResource, identifier: String = "es") -> String {
        var resource = resource
        resource.locale = Locale(identifier: identifier)
        return String(localized: resource)
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
}
