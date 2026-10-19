import Foundation
import Testing
@testable import Bet

/// Bets date headers relative to a fixed now: Sunday, 18 October 2026, 14:00 in Bogotá.
struct PendingBetDayLabelTests {
    private let bogota = TimeZone(identifier: "America/Bogota")!

    private func calendar() -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = bogota
        return calendar
    }

    private func date(_ year: Int, _ month: Int, _ day: Int, hour: Int = 14) -> Date {
        calendar().date(from: DateComponents(year: year, month: month, day: day, hour: hour))!
    }

    private var now: Date { date(2026, 10, 18) }

    private func label(_ day: Date, _ localeIdentifier: String = "en_US") -> String {
        PendingBetDayLabel(calendar: calendar(), locale: Locale(identifier: localeIdentifier))
            .text(for: day, now: now)
    }

    @Test
    func nearDaysAreNamedRelativeToToday() {
        #expect(label(date(2026, 10, 18, hour: 23)) == "Today")
        #expect(label(date(2026, 10, 19, hour: 0)) == "Tomorrow")
        #expect(label(date(2026, 10, 17)) == "Yesterday")
    }

    @Test
    func otherDaysOfTheNextSixAreWeekdays() {
        #expect(label(date(2026, 10, 21)) == "Wednesday")
        #expect(label(date(2026, 10, 24)) == "Saturday")
    }

    @Test
    func laterDaysThisYearAreShortNamedDatesWithoutTheYear() {
        #expect(label(date(2026, 10, 25)) == "Sun, Oct 25")
        #expect(label(date(2026, 10, 30)) == "Fri, Oct 30")
        #expect(label(date(2026, 10, 10)) == "Sat, Oct 10")
    }

    @Test
    func datesInAnotherYearIncludeTheYear() {
        #expect(label(date(2027, 1, 6)) == "Wed, Jan 6, 2027")
    }

    @Test
    func theDayFollowsTheCalendarTimeZone() {
        // 03:00 UTC on 19 October is still 18 October, 22:00, in Bogotá.
        let lateEvening = Date(timeIntervalSince1970: 1_792_378_800)
        #expect(label(lateEvening) == "Today")
    }

    @Test(arguments: ["es_CO", "es_ES"])
    func spanishHeadersAreCapitalizedHeadings(localeIdentifier: String) {
        #expect(label(date(2026, 10, 18), localeIdentifier) == "Hoy")
        #expect(label(date(2026, 10, 19), localeIdentifier) == "Mañana")
        #expect(label(date(2026, 10, 17), localeIdentifier) == "Ayer")
        #expect(label(date(2026, 10, 21), localeIdentifier) == "Miércoles")

        let shortDate = label(date(2026, 10, 30), localeIdentifier)
        #expect(shortDate.hasPrefix("Vie"))
        #expect(shortDate.contains("30"))
        #expect(shortDate.contains("oct"))
        #expect(!shortDate.contains("2026"))
        #expect(label(date(2027, 1, 6), localeIdentifier).contains("2027"))
    }
}

/// The Bets list draws each kickoff day as one section from the day's first loaded match.
struct PendingBetDayRunTests {
    private let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/Bogota")!
        return calendar
    }()

    private let matches: [PoolGamblerBetModel?] = [
        PendingBetFixtures.match("a", kickoffDay: 0),
        PendingBetFixtures.match("b", kickoffDay: 0),
        PendingBetFixtures.match("c", kickoffDay: 1),
        nil,
        PendingBetFixtures.match("d", kickoffDay: 1),
    ]

    private func run(_ index: Int) -> [String]? {
        pendingBetDayRun(startingAt: index, calendar: calendar) { matches.indices.contains($0) ? matches[$0] : nil }?
            .map(\.matchId)
    }

    @Test
    func theFirstMatchOfADayCarriesTheWholeDay() {
        #expect(run(0) == ["a", "b"])
        #expect(run(1) == nil)
        #expect(run(2) == ["c"])
    }

    @Test
    func anUnloadedSlotEndsTheRunAndStartsNothing() {
        #expect(run(3) == nil)
        #expect(run(4) == ["d"])
    }
}
