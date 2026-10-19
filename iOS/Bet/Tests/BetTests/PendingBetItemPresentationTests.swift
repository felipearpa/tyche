import Core
import DataBet
import Foundation
import SwiftUI
import Testing
import UI
import ViewInspector
@testable import Bet

/// The match card as each row state draws it: the status its summary element reads, the controls
/// it offers, and the fixed-size status slot. Inspected with ViewInspector, so it runs on the
/// iOS 18.1 simulator.
@MainActor
struct PendingBetItemPresentationTests {
    private let match = PendingBetFixtures.match("m1").copy {
        $0.homeTeamName = "Colombia"
        $0.awayTeamName = "Portugal"
    }
    private let actions = PendingBetItemActions(
        setHome: { _ in }, setAway: { _ in }, step: { _, _ in }, retry: {}, focusEnter: {}, focusLeave: {}
    )

    private struct Expected {
        let state: PendingBetRowState
        let draft: DraftScores
        let status: String
        let buttons: Int
        let scoreFields: Int
        let showsSpinner: Bool
        let texts: [String]
        let absentTexts: [String]
    }

    /// Four steppers on every open card; Retry on a failure; nothing on a locked card. No quick
    /// picks and no hint line in any state.
    private let expectations: [Expected] = [
        Expected(state: .empty, draft: DraftScores(nil, nil), status: "No bet", buttons: 4, scoreFields: 2,
                 showsSpinner: false, texts: [], absentTexts: ["1–0", "2–1"]),
        Expected(state: .incomplete, draft: DraftScores(2, nil), status: "Incomplete", buttons: 4, scoreFields: 2,
                 showsSpinner: false, texts: [], absentTexts: ["Add Portugal's score to save", "1–0"]),
        Expected(state: .saving, draft: DraftScores(2, 1), status: "Saving", buttons: 4, scoreFields: 2,
                 showsSpinner: true, texts: [], absentTexts: ["1–0"]),
        Expected(state: .saved, draft: DraftScores(2, 1), status: "Saved", buttons: 4, scoreFields: 2,
                 showsSpinner: false, texts: ["Saved"], absentTexts: []),
        Expected(state: .failed, draft: DraftScores(2, 1), status: "Not saved", buttons: 5, scoreFields: 2,
                 showsSpinner: false, texts: ["Not saved", "Couldn't save. Your bet is kept here.", "Retry"],
                 absentTexts: ["1–0"]),
        Expected(state: .locked, draft: DraftScores(2, 0), status: "Locked", buttons: 0, scoreFields: 0,
                 showsSpinner: false, texts: ["Locked", "2", "0"], absentTexts: []),
    ]

    @Test
    func eachStateShowsItsStatusAndControls() throws {
        for expected in expectations {
            let item = PendingBetItem(
                match: expected.state == .locked ? match.copy { $0.lock() } : match,
                draft: (home: expected.draft.home, away: expected.draft.away),
                rowState: expected.state,
                actions: actions
            )
            let view = try item.inspect()

            #expect(try summaryValue(in: view) == expected.status, "\(expected.state)")
            let controls = Set(view.findAll(ViewType.Button.self).map(buttonName))
            #expect(controls.count == expected.buttons, "\(expected.state)")
            #expect(view.findAll(ViewType.TextField.self).count == expected.scoreFields, "\(expected.state)")
            #expect(((try? view.find(BallSpinner.self)) != nil) == expected.showsSpinner, "\(expected.state)")
            for text in expected.texts {
                #expect((try? view.find(text: text)) != nil, "\(expected.state): \(text)")
            }
            for text in expected.absentTexts {
                #expect((try? view.find(text: text)) == nil, "\(expected.state): \(text)")
            }
        }
    }

    @Test
    func steppersAndBoxesNameTheirTeams() throws {
        let view = try PendingBetItem(match: match, draft: (nil, nil), rowState: .empty, actions: actions).inspect()
        let labels = try view.findAll(ViewType.Button.self).map { try $0.accessibilityLabel().string() }

        #expect(labels.contains("One more goal for Colombia"))
        #expect(labels.contains("One less goal for Portugal"))
        let fields = try view.findAll(ViewType.TextField.self).map { try $0.accessibilityLabel().string() }
        #expect(fields == ["Colombia goals", "Portugal goals"])
        #expect(try view.findAll(ViewType.TextField.self)[0].accessibilityValue().string() == "No score")
    }

    @Test
    func theMissingBoxOfAnIncompleteCardSaysWhoseScoreIsNeeded() throws {
        let view = try PendingBetItem(match: match, draft: (2, nil), rowState: .incomplete, actions: actions).inspect()
        let fields = view.findAll(ViewType.TextField.self)

        #expect(try fields[0].accessibilityValue().string() == "2")
        #expect(try fields[1].accessibilityLabel().string() == "Portugal goals")
        #expect(try fields[1].accessibilityValue().string() == "Add Portugal's score to save")
    }

    @Test
    func steppersReachTheActions() throws {
        var steps: [String] = []
        let recording = PendingBetItemActions(
            setHome: { _ in }, setAway: { _ in },
            step: { team, delta in steps.append("\(team) \(delta)") },
            retry: {}, focusEnter: {}, focusLeave: {}
        )
        let view = try PendingBetItem(match: match, draft: (nil, nil), rowState: .empty, actions: recording).inspect()

        try view.findAll(ViewType.Button.self)
            .first { try $0.accessibilityLabel().string() == "One more goal for Portugal" }?.tap()

        #expect(steps == ["away 1"])
    }

    @Test
    func aPlaceholderCardExposesNoControls() throws {
        let view = try PendingBetPlaceholderRow().inspect()

        #expect(view.findAll(ViewType.Button.self).isEmpty)
        #expect(view.findAll(ViewType.TextField.self).isEmpty)
        let item = try view.find(PendingBetItem.self).actualView()
        #expect(item.isPlaceholder)
        #expect(item.actions == nil)
    }

    @Test
    func theStatusSlotKeepsItsHeightBetweenSavingAndSaved() throws {
        let heights = try [PendingBetRowState.saving, .saved, .empty, .locked].map { state in
            try slotHeight(PendingBetStatusSlot(rowState: state, isPlaceholder: false))
        }

        #expect(heights == [26, 26, 26, 26])
    }

    @Test
    func theStatusPillHoldsTheBallWhileSavingAndAnIconAndTextOnceSaved() throws {
        let saving = try PendingBetStatusSlot(rowState: .saving, isPlaceholder: false).inspect()
        let saved = try PendingBetStatusSlot(rowState: .saved, isPlaceholder: false).inspect()
        let noBet = try PendingBetStatusSlot(rowState: .empty, isPlaceholder: false).inspect()

        #expect((try? saving.find(BallSpinner.self)) != nil)
        #expect((try? saving.find(text: "Saving")) == nil)
        #expect((try? saved.find(BallSpinner.self)) == nil)
        #expect((try? saved.find(text: "Saved")) != nil)
        #expect(saved.findAll(ViewType.Image.self).isEmpty == false)
        #expect((try? noBet.find(text: "No bet")) != nil)
        #expect(noBet.findAll(ViewType.Image.self).isEmpty)
    }

    @Test
    func theCardValueMovesFromSavingToSaved() async throws {
        let repository = ScriptedPendingBetRepository()
        let clock = ManualClock()
        let outcomes = PendingBetOutcomeLog()
        let row = PendingBetRowHolder(
            match: match,
            betUseCase: BetUseCase(poolGamblerBetRepository: repository),
            outcomes: outcomes,
            clock: clock
        )
        let card = PendingBetItemView(holder: row, onFocusEnter: {})

        row.setBoth(home: 2, away: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        let submitted = try await repository.nextSubmission()
        #expect(try summaryValue(in: card.inspect()) == "Saving")

        repository.completeSaving(submitted, as: match)
        try #require(await eventually { row.rowState == .saved })
        #expect(try summaryValue(in: card.inspect()) == "Saved")
    }

    @Test
    func aForbiddenSaveTurnsTheCardIntoTheLockedCardWithTheSavedScores() async throws {
        let repository = ScriptedPendingBetRepository()
        let clock = ManualClock()
        let saved = match.copy { $0.betScore = TeamScore(homeTeamValue: 2, awayTeamValue: 1) }
        let row = PendingBetRowHolder(
            match: saved,
            betUseCase: BetUseCase(poolGamblerBetRepository: repository),
            outcomes: PendingBetOutcomeLog(),
            clock: clock
        )
        let card = PendingBetItemView(holder: row, onFocusEnter: {})

        row.setHome(3)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        _ = try await repository.nextSubmission()
        #expect(try card.inspect().findAll(ViewType.TextField.self).count == 2)

        repository.complete(with: .failure(BetError.forbidden))
        try #require(await eventually { row.rowState == .locked })

        let locked = try card.inspect()
        #expect(try summaryValue(in: locked) == "Locked")
        #expect(locked.findAll(ViewType.Button.self).isEmpty)
        #expect(locked.findAll(ViewType.TextField.self).isEmpty)
        let scores = locked.findAll(BetScoreReadOnlyBox.self).compactMap { try? $0.actualView().value }
        #expect(scores == [2, 1])
    }

    @Test
    func filterChipsShowAPlusAndReadAtLeastWhileMorePagesRemain() throws {
        let counts = PendingBetFilterCounts(all: 6, noBet: 2, placed: 4)
        let partial = try PendingBetFilterRow(selected: .all, counts: counts, isPartial: true, onSelect: { _ in }).inspect()
        let exact = try PendingBetFilterRow(selected: .all, counts: counts, isPartial: false, onSelect: { _ in }).inspect()

        #expect((try? partial.find(text: "2+")) != nil)
        #expect((try? exact.find(text: "2+")) == nil)
        #expect((try? exact.find(text: "2")) != nil)
        let values = partial.findAll(ViewType.Button.self).compactMap { try? $0.accessibilityValue().string() }
        #expect(values.contains("at least 2 matches"))
    }

    @Test
    func exactAndPartialCountsShareTheLocaleNumberFormat() throws {
        let counts = PendingBetFilterCounts(all: 3000, noBet: 1123, placed: 1877)
        let exact = try PendingBetFilterRow(selected: .all, counts: counts, isPartial: false, onSelect: { _ in }).inspect()
        let partial = try PendingBetFilterRow(selected: .all, counts: counts, isPartial: true, onSelect: { _ in }).inspect()

        #expect((try? exact.find(text: 3000.formatted(.number))) != nil)
        #expect((try? partial.find(text: String(localized: .betsFilterCountPartial(1123)))) != nil)
        #expect(String(localized: .betsFilterCountPartial(1123)).hasPrefix(1123.formatted(.number)))
    }

    // MARK: Score box input

    @Test
    func typedScoresKeepUpToTwoDigitsWithoutLeadingZeros() {
        #expect(parseBetScoreInput("") == nil)
        #expect(parseBetScoreInput("2") == 2)
        #expect(parseBetScoreInput("05") == 5)
        #expect(parseBetScoreInput("00") == 0)
        #expect(parseBetScoreInput("0") == 0)
        #expect(parseBetScoreInput("123") == 12)
        #expect(parseBetScoreInput("1a2") == 12)
        #expect(parseBetScoreInput("-3") == 3)
        #expect(parseBetScoreInput("٣") == nil)
        #expect(parseBetScoreInput("abc") == nil)
    }

    @Test
    func theScoreBoxReportsTheFilteredValueOfTypedText() throws {
        var reported: [Int?] = []
        let box = BetScoreBox(value: nil, otherValue: nil, teamName: "Colombia", isFocused: true) { reported.append($0) }
        let field = try box.inspect().find(ViewType.TextField.self)

        try field.callOnChange(newValue: "07")
        try field.callOnChange(newValue: "x")
        #expect(reported == [7])

        var cleared: [Int?] = []
        let filled = BetScoreBox(value: 4, otherValue: nil, teamName: "Colombia", isFocused: true) { cleared.append($0) }
        try filled.inspect().find(ViewType.TextField.self).callOnChange(newValue: "")
        try filled.inspect().find(ViewType.TextField.self).callOnChange(newValue: "4")
        #expect(cleared == [nil])
    }

    @Test
    func aBoxThatHandsOffKeepsOnlyItsFirstTypedDigit() {
        #expect(splitFirstTypedDigit("2") == (2, nil))
        #expect(splitFirstTypedDigit("21") == (2, 1))
        #expect(splitFirstTypedDigit("210") == (2, 10))
        #expect(splitFirstTypedDigit("05") == (0, 5))
        #expect(splitFirstTypedDigit("") == (nil, nil))
    }

    @Test
    func digitsTypedIntoTheHomeBoxAfterTheFirstReachTheAwayBox() throws {
        var home: [Int?] = []
        var away: [Int] = []
        let box = BetScoreBox(
            value: 2, otherValue: nil, teamName: "Colombia", isFocused: true,
            onInput: { home.append($0) }, onOverflow: { away.append($0) }
        )

        // The second keystroke arrives before focus has moved to the away box.
        try box.inspect().find(ViewType.TextField.self).callOnChange(newValue: "21")

        #expect(home.isEmpty)
        #expect(away == [1])

        var fastHome: [Int?] = []
        var fastAway: [Int] = []
        let empty = BetScoreBox(
            value: nil, otherValue: nil, teamName: "Colombia", isFocused: true,
            onInput: { fastHome.append($0) }, onOverflow: { fastAway.append($0) }
        )
        try empty.inspect().find(ViewType.TextField.self).callOnChange(newValue: "21")
        #expect(fastHome == [2])
        #expect(fastAway == [1])
    }

    @Test
    func aStepperSetTwoDigitHomeScoreIsNotSplit() throws {
        var reported: [Int?] = []
        var overflow: [Int] = []
        let box = BetScoreBox(
            value: 12, otherValue: 1, teamName: "Colombia", isFocused: false,
            onInput: { reported.append($0) }, onOverflow: { overflow.append($0) }
        )
        try box.inspect().find(ViewType.TextField.self).callOnChange(newValue: "12")

        #expect(reported.isEmpty)
        #expect(overflow.isEmpty)
    }

    @Test
    func boxAppearanceFollowsValueFocusAndLock() {
        #expect(BetScoreBoxAppearance(value: nil, otherValue: nil, isFocused: false, isLocked: false) == .empty)
        #expect(BetScoreBoxAppearance(value: nil, otherValue: 2, isFocused: false, isLocked: false) == .missing)
        #expect(BetScoreBoxAppearance(value: 1, otherValue: nil, isFocused: false, isLocked: false) == .filled)
        #expect(BetScoreBoxAppearance(value: 1, otherValue: nil, isFocused: true, isLocked: false) == .focused)
        #expect(BetScoreBoxAppearance(value: 1, otherValue: 1, isFocused: true, isLocked: true) == .locked)
    }

    // MARK: Lock note and announcements

    @Test
    func theLockNoteCountsWholeMinutesDuringTheLastHourBeforeTheLock() {
        let kickoff = PendingBetFixtures.kickoff()
        let lock = kickoff.addingTimeInterval(-600)

        func minutes(_ secondsBeforeLock: TimeInterval) -> Int? {
            lockCountdownMinutes(kickoff: kickoff, now: lock.addingTimeInterval(-secondsBeforeLock))
        }

        #expect(minutes(3601) == nil)
        #expect(minutes(3600) == 60)
        #expect(minutes(3599) == 60)
        #expect(minutes(3540) == 59)
        #expect(minutes(450) == 8)
        #expect(minutes(60) == 1)
        #expect(minutes(59) == 1)
        #expect(minutes(1) == 1)
        #expect(minutes(0) == nil)
        #expect(minutes(-20) == nil)
    }

    @Test
    func theLockNoteUpdatesAtEachWholeMinuteBeforeTheLockAndAtTheLock() {
        let kickoff = PendingBetFixtures.kickoff()
        let lock = kickoff.addingTimeInterval(-600)

        // Days away, the timeline starts now, so the first render shows no note, and wakes at the
        // start of the last hour.
        let farAway = lock.addingTimeInterval(-86_400)
        let fromFarAway = lockCountdownChanges(kickoff: kickoff, after: farAway)
        #expect(fromFarAway.count == 62)
        #expect(fromFarAway.first == farAway)
        #expect(lockCountdownMinutes(kickoff: kickoff, now: farAway) == nil)
        #expect(fromFarAway[1] == lock.addingTimeInterval(-3600))
        #expect(fromFarAway.last == lock)

        // 20 s past a minute boundary, the next update is the next boundary, not 60 s from now.
        let midMinute = lock.addingTimeInterval(-80)
        #expect(lockCountdownChanges(kickoff: kickoff, after: midMinute)
            == [midMinute, lock.addingTimeInterval(-60), lock])

        #expect(lockCountdownChanges(kickoff: kickoff, after: lock) == [lock])
        let pastLock = lock.addingTimeInterval(20)
        #expect(lockCountdownChanges(kickoff: kickoff, after: pastLock) == [pastLock])
    }

    @Test
    func savesAndFailuresAreAnnouncedOnce() {
        let score = TeamScore(homeTeamValue: 2, awayTeamValue: 1)

        #expect(saveAnnouncement(from: .saving, to: .saved, match: match, saved: score)
            == "Bet saved: Colombia 2, Portugal 1")
        #expect(saveAnnouncement(from: .saving, to: .failed, match: match, saved: nil)
            == "Couldn't save your bet on Colombia versus Portugal")
        #expect(saveAnnouncement(from: .saving, to: .locked, match: match, saved: nil)
            == "Betting on Colombia versus Portugal is now closed")
        #expect(saveAnnouncement(from: .saved, to: .locked, match: match, saved: score) == nil)
        #expect(saveAnnouncement(from: .empty, to: .saved, match: match, saved: score) == nil)
        #expect(saveAnnouncement(from: .empty, to: .saving, match: match, saved: nil) == nil)
    }

    /// The fixed height the slot gives its pill.
    private func slotHeight(_ slot: PendingBetStatusSlot) throws -> CGFloat? {
        try slot.inspect().find(where: { (try? $0.fixedHeight()) != nil }).fixedHeight()
    }

    /// A button's accessibility label, or its visible text when it has none.
    private func buttonName(_ button: InspectableView<ViewType.Button>) -> String {
        (try? button.accessibilityLabel().string())
            ?? (try? button.labelView().find(ViewType.Text.self).string())
            ?? ""
    }

    /// The value of the card's summary element, the one labelled with the matchup.
    private func summaryValue<V>(in root: InspectableView<V>) throws -> String {
        let summary = try root.find(where: { view in
            (try? view.accessibilityLabel().string())?.hasPrefix("Colombia versus Portugal") == true
        })
        return try summary.accessibilityValue().string()
    }
}
