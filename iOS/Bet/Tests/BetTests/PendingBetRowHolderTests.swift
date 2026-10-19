import Core
import DataBet
import Foundation
import Testing
import ViewingState
@testable import Bet

/// The prediction row's draft, derived state, steppers, and automatic save, driven through the
/// view model with a manual clock and a repository double so that no prediction reaches a backend.
@MainActor
struct PendingBetRowHolderTests {
    private let repository = ScriptedPendingBetRepository()
    private let clock = ManualClock()
    private let outcomes = PendingBetOutcomeLog()

    private func makeRow(_ match: PoolGamblerBetModel) -> PendingBetRowHolder {
        PendingBetRowHolder(
            match: match,
            betUseCase: BetUseCase(poolGamblerBetRepository: repository),
            outcomes: outcomes,
            clock: clock
        )
    }

    private let empty = PendingBetFixtures.match("m1")
    private let savedTwoOne = PendingBetFixtures.match("m1", betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 1))

    // MARK: Derived state

    @Test
    func aMatchWithoutPredictionStartsEmpty() {
        let row = makeRow(empty)

        #expect(row.rowState == .empty)
        #expect(row.draftScores == DraftScores(nil, nil))
        #expect(row.saved == nil)
    }

    @Test
    func aMatchWithPredictionStartsSavedWithItsScoresInTheDraft() {
        let row = makeRow(savedTwoOne)

        #expect(row.rowState == .saved)
        #expect(row.draftScores == DraftScores(2, 1))
    }

    @Test
    func aLockedMatchIsLockedAndIgnoresEdits() async {
        let row = makeRow(PendingBetFixtures.match("m1", isLocked: true))

        row.setHome(1)
        row.step(.away, by: 1)
        row.setBoth(home: 3, away: 3)
        clock.advance(by: .seconds(5))
        await settle()

        #expect(row.rowState == .locked)
        #expect(row.draftScores == DraftScores(nil, nil))
        #expect(repository.submissionCount == 0)
    }

    @Test
    func oneScoreIsIncompleteAndNeverSubmits() async {
        let row = makeRow(empty)

        row.setHome(2)
        clock.advance(by: .seconds(10))
        await settle()

        #expect(row.rowState == .incomplete)
        #expect(row.draftScores == DraftScores(2, nil))
        #expect(clock.sleeperCount == 0)
        #expect(repository.submissionCount == 0)
    }

    @Test
    func clearingAScoreOfASavedPredictionIsIncompleteAndKeepsTheSavedValue() async {
        let row = makeRow(savedTwoOne)

        row.setAway(nil)
        clock.advance(by: .seconds(10))
        await settle()

        #expect(row.rowState == .incomplete)
        #expect(row.saved == TeamScore(homeTeamValue: 2, awayTeamValue: 1))
        #expect(repository.submissionCount == 0)
    }

    @Test
    func returningToTheSavedScoresCancelsTheSave() async {
        let row = makeRow(savedTwoOne)

        row.setAway(2)
        #expect(row.rowState == .saved)
        row.setAway(1)
        clock.advance(by: .seconds(5))
        await settle()

        #expect(row.rowState == .saved)
        #expect(repository.submissionCount == 0)
    }

    @Test
    func negativeScoresAreRejected() {
        let row = makeRow(savedTwoOne)

        row.setHome(-1)

        #expect(row.draftScores == DraftScores(2, 1))
    }

    // MARK: Steppers

    @Test
    func plusOnAnEmptyCardSetsThatTeamToOneAndTheOtherToZero() {
        let home = makeRow(empty)
        let away = makeRow(PendingBetFixtures.match("m2"))

        home.step(.home, by: 1)
        away.step(.away, by: 1)

        #expect(home.draftScores == DraftScores(1, 0))
        #expect(away.draftScores == DraftScores(0, 1))
        // A complete draft reads as No bet until its debounced save starts.
        #expect(away.rowState == .empty)
    }

    @Test
    func minusOnAnEmptyCardSetsBothToZero() {
        let row = makeRow(empty)

        row.step(.home, by: -1)

        #expect(row.draftScores == DraftScores(0, 0))
    }

    @Test
    func steppersChangeOneScoreAndStopAtZero() {
        let row = makeRow(PendingBetFixtures.match("m1", betScore: TeamScore(homeTeamValue: 0, awayTeamValue: 1)))

        row.step(.away, by: 1)
        row.step(.home, by: -1)

        #expect(row.draftScores == DraftScores(0, 2))
    }

    @Test
    func plusOnTheMissingSideCompletesAnIncompleteDraft() async throws {
        let row = makeRow(empty)
        row.setHome(2)

        row.step(.away, by: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        let submitted = try await repository.nextSubmission()

        #expect(row.draftScores == DraftScores(2, 1))
        #expect(submitted.homeTeamBet.value == 2 && submitted.awayTeamBet.value == 1)
    }

    // MARK: Automatic save

    @Test
    func typingBothScoresSavesOnceAfterTheDebounce() async throws {
        let row = makeRow(empty)
        let history = RowStateHistory(row)

        row.setHome(2)
        row.setAway(1)
        try await clock.waitForSleepers(1)
        clock.advance(by: .milliseconds(699))
        await settle()
        #expect(repository.submissionCount == 0)
        #expect(row.rowState == .empty)

        clock.advance(by: .milliseconds(1))
        let submitted = try await repository.nextSubmission()
        #expect(row.rowState == .saving)
        #expect(row.saveState == .mutating(original: .none, updated: .placed(TeamScore(homeTeamValue: 2, awayTeamValue: 1))))
        #expect(submitted.matchId == "m1")
        #expect(submitted.homeTeamBet.value == 2 && submitted.awayTeamBet.value == 1)

        repository.completeSaving(submitted, as: empty)
        #expect(await eventually { row.rowState == .saved })
        #expect(row.saved == TeamScore(homeTeamValue: 2, awayTeamValue: 1))
        #expect(repository.submissionCount == 1)
        #expect(outcomes.events == [.saved("m1")])
        #expect(row.saveState == .mutated(original: .none, updated: .placed(TeamScore(homeTeamValue: 2, awayTeamValue: 1))))
        #expect(history.states == [.empty, .incomplete, .empty, .saving, .saved])
    }

    @Test
    func threeStepperTapsWithin300MillisecondsMakeOneSubmission() async throws {
        let original = PendingBetFixtures.match("m1", betScore: TeamScore(homeTeamValue: 0, awayTeamValue: 0))
        let row = makeRow(original)
        let history = RowStateHistory(row)

        row.step(.home, by: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: .milliseconds(150))
        row.step(.home, by: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: .milliseconds(150))
        row.step(.home, by: 1)
        try await clock.waitForSleepers(1)
        #expect(row.rowState == .saved)
        #expect(history.states == [.saved])
        clock.advance(by: PendingBetRowHolder.saveDebounce)

        let submitted = try await repository.nextSubmission()
        repository.completeSaving(submitted, as: original)
        #expect(await eventually { row.rowState == .saved })
        await settle()

        #expect(submitted.homeTeamBet.value == 3 && submitted.awayTeamBet.value == 0)
        #expect(repository.submissionCount == 1)
        #expect(row.saved == TeamScore(homeTeamValue: 3, awayTeamValue: 0))
        #expect(history.states == [.saved, .saving, .saved])
    }

    @Test
    func anEditDuringTheFlightResavesTheNewestScoresOnce() async throws {
        let row = makeRow(empty)
        let history = RowStateHistory(row)
        row.setBoth(home: 2, away: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        let first = try await repository.nextSubmission()

        row.setAway(2)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        await settle()
        #expect(repository.submissionCount == 1)
        #expect(row.rowState == .saving)

        repository.completeSaving(first, as: empty)
        let second = try await repository.nextSubmission()
        #expect(second.homeTeamBet.value == 2 && second.awayTeamBet.value == 2)
        // The resave goes from one request straight to the next, from the newly saved scores.
        #expect(row.saveState == .mutating(
            original: .placed(TeamScore(homeTeamValue: 2, awayTeamValue: 1)),
            updated: .placed(TeamScore(homeTeamValue: 2, awayTeamValue: 2))
        ))

        repository.completeSaving(second, as: empty)
        #expect(await eventually { row.rowState == .saved })
        await settle()

        #expect(repository.submissionCount == 2)
        #expect(row.saved == TeamScore(homeTeamValue: 2, awayTeamValue: 2))
        #expect(row.draftScores == DraftScores(2, 2))
        // The spinner stays up from the first request through the resave.
        #expect(history.states == [.empty, .saving, .saved])
    }

    @Test
    func changingBackToTheSavedScoresDuringTheFlightSavesThemAgain() async throws {
        let row = makeRow(savedTwoOne)
        row.setAway(2)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        let first = try await repository.nextSubmission()

        row.setAway(1)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        repository.completeSaving(first, as: savedTwoOne)
        let second = try await repository.nextSubmission()
        repository.completeSaving(second, as: savedTwoOne)

        #expect(await eventually { row.rowState == .saved })
        #expect(second.homeTeamBet.value == 2 && second.awayTeamBet.value == 1)
        #expect(row.saved == TeamScore(homeTeamValue: 2, awayTeamValue: 1))
    }

    // MARK: Failure and retry

    @Test
    func aFailedSaveKeepsTheDraftAndReportsTheFailure() async throws {
        let row = makeRow(empty)

        try await failSave(of: row, home: 4, away: 1)

        #expect(row.rowState == .failed)
        #expect(row.draftScores == DraftScores(4, 1))
        #expect(row.saved == nil)
        #expect(row.saveState.isFailure())
        #expect(row.saveState.attemptedValue() == .placed(TeamScore(homeTeamValue: 4, awayTeamValue: 1)))
        #expect(row.saveState.activeValue() == .none)
        #expect(outcomes.events == [.failed("m1")])
    }

    @Test
    func retryResubmitsTheSameScoresAtOnce() async throws {
        let row = makeRow(empty)
        try await failSave(of: row, home: 4, away: 1)

        row.retry()
        #expect(row.rowState == .saving)
        let retried = try await repository.nextSubmission()
        repository.completeSaving(retried, as: empty)

        #expect(await eventually { row.rowState == .saved })
        #expect(retried.homeTeamBet.value == 4 && retried.awayTeamBet.value == 1)
        #expect(repository.submissionCount == 2)
        #expect(outcomes.events == [.failed("m1"), .failureCleared("m1"), .saved("m1")])
    }

    @Test
    func editingAFailedCardClearsTheFailureAndSavesTheNewScores() async throws {
        let row = makeRow(empty)
        try await failSave(of: row, home: 4, away: 1)

        row.step(.away, by: 1)
        #expect(row.rowState == .empty)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        let resaved = try await repository.nextSubmission()

        #expect(resaved.homeTeamBet.value == 4 && resaved.awayTeamBet.value == 2)
        #expect(outcomes.events == [.failed("m1"), .failureCleared("m1")])
    }

    @Test
    func retryDoesNothingOutsideAFailure() async {
        let row = makeRow(savedTwoOne)

        row.retry()
        await settle()

        #expect(row.rowState == .saved)
        #expect(repository.submissionCount == 0)
    }

    @Test
    func aForbiddenSaveLocksTheRowWithTheLastSavedPrediction() async throws {
        let row = makeRow(savedTwoOne)
        row.setHome(3)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        _ = try await repository.nextSubmission()

        repository.complete(with: .failure(BetError.forbidden))

        #expect(await eventually { row.rowState == .locked })
        #expect(row.draftScores == DraftScores(2, 1))
        #expect(row.saved == TeamScore(homeTeamValue: 2, awayTeamValue: 1))
        #expect(!row.isEditable)
        #expect(outcomes.events == [.locked("m1")])
        #expect(row.saveState == .idle(.placed(TeamScore(homeTeamValue: 2, awayTeamValue: 1))))

        row.retry()
        await settle()
        #expect(repository.submissionCount == 1)
    }

    @Test
    func aForbiddenFirstSaveLocksTheRowWithEmptyScores() async throws {
        let row = makeRow(empty)
        row.setBoth(home: 1, away: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        _ = try await repository.nextSubmission()

        repository.complete(with: .failure(BetError.forbidden))

        #expect(await eventually { row.rowState == .locked })
        #expect(row.draftScores == DraftScores(nil, nil))
        #expect(row.saveState == .idle(.none))
        #expect(!row.saveState.isFailure())
    }

    @Test
    func aForbiddenSaveRestoresTheDraftAndLocksInOneChange() async throws {
        let row = makeRow(savedTwoOne)
        row.setHome(3)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        _ = try await repository.nextSubmission()
        await settle()
        let rendered = RenderedRowLog(row)

        repository.complete(with: .failure(BetError.forbidden))
        try #require(await eventually { row.rowState == .locked })
        await settle()

        // Lock, saved state, and restored draft land in one main-actor turn, so one update shows
        // all of them and the card animates them in one transaction.
        #expect(rendered.distinctFrames == [RenderedRowLog.Frame(state: .locked, draft: DraftScores(2, 1))])
    }

    // MARK: Focus loss

    @Test
    func leavingACardWithBothScoresClearedRestoresTheSavedPrediction() async throws {
        let row = makeRow(savedTwoOne)
        row.setAway(2)
        try await clock.waitForSleepers(1)
        row.setHome(nil)
        row.setAway(nil)

        row.restoreSavedIfCleared()
        clock.advance(by: .seconds(5))
        await settle()

        #expect(row.draftScores == DraftScores(2, 1))
        #expect(row.rowState == .saved)
        #expect(clock.sleeperCount == 0)
        #expect(repository.submissionCount == 0)
    }

    @Test
    func leavingACardWithOneScoreClearedKeepsItIncomplete() {
        let row = makeRow(savedTwoOne)
        row.setAway(nil)

        row.restoreSavedIfCleared()

        #expect(row.draftScores == DraftScores(2, nil))
        #expect(row.rowState == .incomplete)
    }

    @Test
    func leavingAnEmptyCardWithoutPredictionChangesNothing() {
        let row = makeRow(empty)

        row.restoreSavedIfCleared()

        #expect(row.draftScores == DraftScores(nil, nil))
        #expect(row.rowState == .empty)
    }

    // MARK: Feed updates

    @Test
    func aFeedUpdateKeepsAnIncompleteDraft() {
        let row = makeRow(empty)
        row.setHome(2)

        row.bind(empty)

        #expect(row.draftScores == DraftScores(2, nil))
        #expect(row.rowState == .incomplete)
    }

    @Test
    func aFeedUpdateAdoptsANewServerPredictionWhenTheRowHasNoDraft() {
        let row = makeRow(empty)

        row.bind(savedTwoOne)

        #expect(row.saved == TeamScore(homeTeamValue: 2, awayTeamValue: 1))
        #expect(row.draftScores == DraftScores(2, 1))
        #expect(row.rowState == .saved)
    }

    @Test
    func aFeedUpdateThatLocksTheMatchDropsTheDraft() async {
        let row = makeRow(savedTwoOne)
        row.setHome(5)

        row.bind(savedTwoOne.copy { $0.lock() })
        clock.advance(by: .seconds(5))
        await settle()

        #expect(row.rowState == .locked)
        #expect(row.draftScores == DraftScores(2, 1))
        #expect(repository.submissionCount == 0)
    }

    private func failSave(of row: PendingBetRowHolder, home: Int, away: Int) async throws {
        row.setBoth(home: home, away: away)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        _ = try await repository.nextSubmission()
        repository.complete(with: .failure(URLError(.notConnectedToInternet)))
        try #require(await eventually { row.rowState == .failed })
    }
}
