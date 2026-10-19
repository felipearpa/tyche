import Foundation
import Core
import DataBet
import ViewingState

/// Receives the outcomes of a `PendingBetRowHolder`'s saves, so the row does not depend on
/// the screen that keeps it.
@MainActor
protocol PendingBetRowOutcomes: AnyObject {
    /// A prediction was saved; the row's `saved` holds it.
    func saved(_ matchId: String)
    /// A save failed and the row now shows the failure with the attempted scores kept.
    func failed(_ matchId: String)
    /// The row left a failure, by an edit or a retry.
    func failureCleared(_ matchId: String)
    /// The server rejected a save because predictions are locked; the row is now read-only.
    func locked(_ matchId: String)
}

/// One pending match's prediction: the gambler's draft, the last saved prediction, and the
/// automatic save between them. Kept by the list view model, keyed by match, so the draft and an
/// in-flight save survive paging updates and filtering.
///
/// The save itself is a `MutationState` over the saved prediction: `.idle` holds the saved
/// prediction (`.none` before the first save), `.mutating` an in-flight request from it to the
/// attempted scores,
/// `.mutated` the scores the server confirmed, and `.failure` the attempted scores it did not
/// accept. Its active value is always the saved prediction.
///
/// A complete draft that differs from `saved` is submitted `saveDebounce` after the last change.
/// Changes during the debounce restart it; a debounce that ends while a save is in flight queues
/// one resave of the newest draft, sent as soon as that save returns so the row stays saving
/// throughout. An incomplete draft is never submitted.
@MainActor
final class PendingBetRowHolder: ObservableObject {
    typealias Draft = (home: Int?, away: Int?)

    enum Team {
        case home
        case away
    }

    enum RowState: Equatable {
        /// Predictions are closed; the row is read-only.
        case locked
        /// No prediction is saved and the draft is not incomplete: both scores are empty, or a
        /// complete draft is waiting for its debounced save.
        case empty
        /// Exactly one score is entered.
        case incomplete
        /// A save request is in flight.
        case saving
        /// A prediction is saved. A changed complete draft waiting for its debounced save, and
        /// a draft with both scores cleared before focus leaves the card, keep this state.
        case saved
        /// The last save failed; the draft keeps the attempted scores.
        case failed
    }

    static let saveDebounce: Duration = .milliseconds(700)

    @Published private(set) var match: PoolGamblerBetModel
    @Published private(set) var draft: Draft
    @Published private(set) var saveState: MutationState<SavedBet>
    private var isDebouncing = false

    private let betUseCase: BetUseCase
    private let clock: any Clock<Duration>
    private weak var outcomes: PendingBetRowOutcomes?
    private var debounceTask: Task<Void, Never>?
    private var pendingResave = false
    /// The feed's last copy of the match. Each page append re-sends every loaded copy; an
    /// unchanged copy is stale next to a save made since, so only a changed copy is taken.
    private var lastFeedModel: PoolGamblerBetModel

    init(
        match: PoolGamblerBetModel,
        betUseCase: BetUseCase,
        outcomes: PendingBetRowOutcomes,
        clock: any Clock<Duration> = ContinuousClock()
    ) {
        self.match = match
        self.lastFeedModel = match
        self.saveState = .idle(SavedBet(match.betScore))
        self.draft = Self.draft(from: match.betScore)
        self.betUseCase = betUseCase
        self.outcomes = outcomes
        self.clock = clock
    }

    deinit {
        debounceTask?.cancel()
    }

    /// The debounce window shows no state of its own: until the request starts, a complete draft
    /// reads as the prediction the card already has, saved or not.
    var rowState: RowState {
        if match.isLocked { return .locked }
        if saveState.isMutating() { return .saving }
        if saveState.isFailure() { return .failed }
        let isIncomplete = (draft.home == nil) != (draft.away == nil)
        if isIncomplete { return .incomplete }
        return saved == nil ? .empty : .saved
    }

    var isEditable: Bool { !match.isLocked }

    /// The scores of the last prediction the server accepted, or nil when none is saved.
    var saved: TeamScore<Int>? { saveState.activeValue().score }
    /// Takes the feed's latest copy of the match, ignoring a copy the feed already delivered. Server
    /// values replace the prediction only while the row has no draft, save, or failure of its
    /// own; a lock applies either way.
    func bind(_ model: PoolGamblerBetModel) {
        guard model != lastFeedModel else { return }
        lastFeedModel = model
        if model.isLocked {
            adoptLocked(model)
            return
        }
        let current = match.isLocked ? model.copy { $0.lock() } : model
        if match != current { match = current }
        guard !hasLocalWork, saved != model.betScore else { return }
        saveState = .idle(SavedBet(model.betScore))
        draft = Self.draft(from: model.betScore)
    }

    func setHome(_ value: Int?) {
        setDraft((home: value, away: draft.away))
    }

    func setAway(_ value: Int?) {
        setDraft((home: draft.home, away: value))
    }

    func setBoth(home: Int, away: Int) {
        setDraft((home: home, away: away))
    }

    /// Changes `team`'s score by `delta`, never below zero. With neither score entered, `+` sets
    /// the team to 1 and the other to 0, and `−` sets both to 0.
    func step(_ team: Team, by delta: Int) {
        guard delta != 0 else { return }
        guard draft.home != nil || draft.away != nil else {
            setDraft(Self.firstStep(team, delta: delta))
            return
        }
        switch team {
        case .home: setHome(max(0, (draft.home ?? 0) + delta))
        case .away: setAway(max(0, (draft.away ?? 0) + delta))
        }
    }

    /// Called when focus leaves the card. A draft with both scores cleared returns to the saved
    /// prediction without a submission; a draft with one score cleared stays incomplete.
    func restoreSavedIfCleared() {
        guard isEditable, draft.home == nil, draft.away == nil, let saved else { return }
        draft = Self.draft(from: saved)
    }

    /// Resubmits the failed scores at once.
    func retry() {
        guard isEditable, saveState.isFailure(), let score = completeDraft else { return }
        submit(score)
    }

    private var completeDraft: TeamScore<Int>? {
        guard let home = draft.home, let away = draft.away else { return nil }
        return TeamScore(homeTeamValue: home, awayTeamValue: away)
    }

    /// The scores of the request in flight, if any.
    private var inFlight: TeamScore<Int>? {
        guard case .mutating(_, .placed(let updated)) = saveState else { return nil }
        return updated
    }

    private var hasLocalWork: Bool {
        isDebouncing || saveState.isMutating() || saveState.isFailure() || draft != Self.draft(from: saved)
    }

    private func setDraft(_ newDraft: Draft) {
        guard isEditable, Self.isValid(newDraft.home), Self.isValid(newDraft.away) else { return }
        guard newDraft != draft else { return }
        draft = newDraft
        clearFailure()
        scheduleSave()
    }

    private func scheduleSave() {
        cancelDebounce()
        // An in-flight save of these scores already covers them; otherwise the newest saved value
        // is the one to differ from.
        guard let score = completeDraft, score != (inFlight ?? saved) else { return }
        isDebouncing = true
        debounceTask = Task { [weak self, clock] in
            do {
                try await clock.sleep(for: Self.saveDebounce)
            } catch {
                return
            }
            guard !Task.isCancelled else { return }
            self?.debounceElapsed()
        }
    }

    private func cancelDebounce() {
        debounceTask?.cancel()
        debounceTask = nil
        isDebouncing = false
    }

    private func debounceElapsed() {
        debounceTask = nil
        isDebouncing = false
        guard !saveState.isMutating() else {
            pendingResave = true
            return
        }
        guard let score = completeDraft, score != saved else { return }
        submit(score)
    }

    private func submit(_ score: TeamScore<Int>) {
        let wasFailure = saveState.isFailure()
        saveState = .mutating(original: saveState.activeValue(), updated: .placed(score))
        if wasFailure { outcomes?.failureCleared(match.matchId) }
        startRequest(for: score)
    }

    private func startRequest(for score: TeamScore<Int>) {
        // The setters admit only valid scores, so both conversions succeed.
        let bet = Bet(
            poolId: match.poolId,
            gamblerId: match.gamblerId,
            matchId: match.matchId,
            homeTeamBet: BetScore(score.homeTeamValue)!,
            awayTeamBet: BetScore(score.awayTeamValue)!
        )
        Task { [weak self, betUseCase] in
            let result = await betUseCase.execute(bet: bet)
            self?.finishSubmission(of: score, with: result)
        }
    }

    /// A queued resave moves the state from one `.mutating` straight to the next, so the row never
    /// leaves `saving` between a save and its resave.
    private func finishSubmission(of score: TeamScore<Int>, with result: Result<PoolGamblerBet, Error>) {
        switch result {
        case .success(let updated):
            saveSucceeded(updated.toPoolGamblerBetModel())
        case .failure(BetError.forbidden):
            lockAfterRejection()
        case .failure(let error):
            saveFailed(attempted: score, error: error)
        }
    }

    private func saveSucceeded(_ updated: PoolGamblerBetModel) {
        match = updated
        let accepted = SavedBet(updated.betScore)
        if let resave = takeQueuedResave(over: accepted.score) {
            saveState = .mutating(original: accepted, updated: .placed(resave))
            startRequest(for: resave)
        } else {
            saveState = .mutated(original: saveState.activeValue(), updated: accepted)
        }
        outcomes?.saved(updated.matchId)
    }

    private func saveFailed(attempted: TeamScore<Int>, error: Error) {
        let original = saveState.activeValue()
        if let resave = takeQueuedResave(over: original.score) {
            saveState = .mutating(original: original, updated: .placed(resave))
            startRequest(for: resave)
            return
        }
        // A draft edited during the flight supersedes the failed scores: it saves itself or
        // shows as incomplete.
        guard completeDraft == attempted else {
            saveState = .idle(original)
            return
        }
        saveState = .failure(original: original, updated: .placed(attempted), error: error)
        outcomes?.failed(match.matchId)
    }

    /// Takes the queued resave, if any: the newest complete draft when it differs from `saved`.
    private func takeQueuedResave(over saved: TeamScore<Int>?) -> TeamScore<Int>? {
        guard pendingResave else { return nil }
        pendingResave = false
        guard let score = completeDraft, score != saved else { return nil }
        return score
    }

    /// The refused scores are dropped: the row shows the last saved prediction, read-only.
    private func lockAfterRejection() {
        adoptLocked(match.copy { $0.lock() })
        outcomes?.locked(match.matchId)
    }

    /// Makes the row read-only with the last saved prediction, dropping any draft and queued save.
    private func adoptLocked(_ model: PoolGamblerBetModel) {
        cancelDebounce()
        pendingResave = false
        if match != model { match = model }
        let lockedState: MutationState<SavedBet> = .idle(SavedBet(model.betScore))
        if saveState != lockedState { saveState = lockedState }
        let lockedDraft = Self.draft(from: model.betScore)
        if draft != lockedDraft { draft = lockedDraft }
    }

    private func clearFailure() {
        guard saveState.isFailure() else { return }
        saveState = .idle(saveState.activeValue())
        outcomes?.failureCleared(match.matchId)
    }

    private static func draft(from score: TeamScore<Int>?) -> Draft {
        (home: score?.homeTeamValue, away: score?.awayTeamValue)
    }

    private static func firstStep(_ team: Team, delta: Int) -> Draft {
        guard delta > 0 else { return (home: 0, away: 0) }
        switch team {
        case .home: return (home: 1, away: 0)
        case .away: return (home: 0, away: 1)
        }
    }

    private static func isValid(_ value: Int?) -> Bool {
        value.map { BetScore.isValid(value: $0) } ?? true
    }
}

/// The prediction the server holds for a match: none yet, or the placed scores.
enum SavedBet: Equatable {
    case none
    case placed(TeamScore<Int>)

    init(_ score: TeamScore<Int>?) {
        self = score.map(SavedBet.placed) ?? .none
    }

    /// The placed scores, or nil when no bet is saved; for comparing with drafts and feed values.
    var score: TeamScore<Int>? {
        switch self {
        case .none: nil
        case .placed(let score): score
        }
    }
}
