import Core
import DataBet
import Foundation
import Testing
@testable import Bet

/// The match screen's state: the heading and privacy gate, the viewer's own prediction, and the
/// ranked feed, as explored on the iOS 18.1 simulator and the medium_phone emulator (see the
/// change's verification.md, task 2.1).
@MainActor
struct MatchBetListViewModelTests {
    private typealias F = MatchFixtures

    // MARK: Heading and gate

    @Test
    func theGateIsUnknownUntilTheFirstHeadingArrives() async throws {
        let repository = ScriptedMatchRepository()
        repository.holdsBetRequests = true
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true)))
        let viewModel = makeViewModel(repository)

        let load = Task { await viewModel.loadHeading() }
        try await repository.waitForHeldBetRequests(1)
        #expect(viewModel.heading.presentation == .placeholder)
        #expect(viewModel.heading.confirmedPresentation == nil)

        repository.releaseHeldBetRequest(at: 0)
        await load.value
        #expect(viewModel.heading.confirmedPresentation == .betsClosed)
    }

    @Test
    func aFailedRefreshKeepsTheConfirmedHeadingAndSaysItFailed() async {
        let repository = ScriptedMatchRepository()
        let confirmed = F.match(locked: true, computed: true, matchScore: TeamScore(homeTeamValue: 0, awayTeamValue: 0))
        repository.betResponses[F.viewerId] = .success(F.domain(confirmed))
        let viewModel = makeViewModel(repository)
        await viewModel.loadHeading()

        repository.betResponses[F.viewerId] = .failure(URLError(.notConnectedToInternet))
        await viewModel.loadHeading()

        #expect(viewModel.heading.presentation == .loaded(confirmed, .computed, .failed))
    }

    @Test
    func aFirstHeadingFailureOffersRetry() async {
        let repository = ScriptedMatchRepository()
        let viewModel = makeViewModel(repository)
        await viewModel.load()
        #expect(viewModel.heading.presentation == .failed)

        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false)))
        await viewModel.load()
        #expect(viewModel.heading.confirmedPresentation == .hidden)
    }

    @Test
    func anOlderHeadingResponseCannotReplaceANewerOne() async throws {
        let repository = ScriptedMatchRepository()
        repository.holdsBetRequests = true
        let viewModel = makeViewModel(repository)

        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false)))
        let older = Task { await viewModel.loadHeading() }
        try await repository.waitForHeldBetRequests(1)

        let newer = Task { await viewModel.loadHeading() }
        try await repository.waitForHeldBetRequests(2)
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true)))

        // Both requests answer "locked" now; release the newer first, then make the older one
        // answer "open" and release it last.
        repository.releaseHeldBetRequest(at: 1)
        await newer.value
        #expect(viewModel.heading.confirmedPresentation == .betsClosed)

        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false)))
        repository.releaseHeldBetRequest(at: 0)
        await older.value
        #expect(viewModel.heading.confirmedPresentation == .betsClosed)
    }

    // MARK: Own prediction

    @Test
    func whenTheRouteIsTheViewerTheHeadingBetIsTheOwnBetWithoutAnotherRequest() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false)))
        let viewModel = makeViewModel(repository)

        await viewModel.load()

        #expect(viewModel.ownBet == .bet(TeamScore(homeTeamValue: 3, awayTeamValue: 2)))
        #expect(repository.betRequests == [F.viewerId])
    }

    @Test
    func entryFromAnotherGamblersTimelineRequestsTheViewersOwnBet() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.otherId] = .success(
            F.domain(F.match(gamblerId: F.otherId, locked: false, bet: TeamScore(homeTeamValue: 1, awayTeamValue: 0)))
        )
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false)))
        let viewModel = makeViewModel(repository, routedGamblerId: F.otherId)

        await viewModel.load()

        #expect(repository.betRequests == [F.otherId, F.viewerId])
        #expect(viewModel.ownBet == .bet(TeamScore(homeTeamValue: 3, awayTeamValue: 2)))
    }

    @Test
    func ownBetDistinguishesAbsenceFailureAndRetry() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.otherId] = .success(F.domain(F.match(gamblerId: F.otherId, locked: false)))
        repository.betResponses[F.viewerId] = .failure(URLError(.timedOut))
        let viewModel = makeViewModel(repository, routedGamblerId: F.otherId)

        await viewModel.load()
        #expect(viewModel.ownBet == .failed)

        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false, bet: nil)))
        await viewModel.loadOwnBet()
        #expect(viewModel.ownBet == .noBet)

        repository.betResponses[F.viewerId] = .failure(NetworkError.http(code: .notFound))
        await viewModel.loadOwnBet()
        #expect(viewModel.ownBet == .noBet)
    }

    @Test
    func ownBetIsNotRequestedWhileBetsAreRevealed() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.otherId] = .success(F.domain(F.match(gamblerId: F.otherId, locked: true)))
        let viewModel = makeViewModel(repository, routedGamblerId: F.otherId)

        await viewModel.load()

        #expect(repository.betRequests == [F.otherId])
    }

    // MARK: Ranked feed and gate changes

    @Test
    func theFeedKeepsTheServersOrderEvenWhenAwardsDisagree() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true, computed: true)))
        repository.rankedResponder = { _ in
            .success(CursorPage(items: [F.participant("leader", award: 0), F.participant("second", award: 4)], next: nil))
        }
        let viewModel = makeViewModel(repository)
        await viewModel.load()

        await viewModel.lazyPager.refresh()

        #expect(viewModel.lazyPager.loadedItems.map(\.gamblerId) == ["leader", "second"])
        #expect(viewModel.lazyPager.loadedItems.map(\.score) == [0, 4])
    }

    @Test
    func aHiddenAnswerFromTheFeedShowsTheHiddenStateAndDropsRows() async throws {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true)))
        let viewModel = makeViewModel(repository)
        await viewModel.load()
        repository.rankedResponder = { _ in .failure(RankedMatchBetsError.predictionsHidden) }
        let revealedPager = viewModel.lazyPager

        await revealedPager.refresh()

        #expect(viewModel.heading.confirmedPresentation == .hidden)
        #expect(viewModel.lazyPager !== revealedPager)
        #expect(viewModel.lazyPager.itemCount == 0)
    }

    @Test
    func aRescheduledMatchThatOpensAgainDropsRevealedRows() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true)))
        repository.rankedResponder = { _ in F.page(["a", "b"]) }
        let viewModel = makeViewModel(repository)
        await viewModel.load()
        await viewModel.lazyPager.refresh()
        #expect(viewModel.lazyPager.itemCount == 2)

        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: false)))
        await viewModel.refresh()

        #expect(viewModel.heading.confirmedPresentation == .hidden)
        #expect(viewModel.lazyPager.itemCount == 0)
        #expect(viewModel.ownBet == .bet(TeamScore(homeTeamValue: 3, awayTeamValue: 2)))
    }

    @Test
    func refreshReplacesTheFeedFromAFreshFirstPage() async {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true, computed: true)))
        repository.rankedResponder = { next in next == nil ? F.page(["a", "b"], next: "c1") : F.page(["c"]) }
        let viewModel = makeViewModel(repository)
        await viewModel.load()
        await viewModel.lazyPager.refresh()
        await viewModel.lazyPager.appendIfNeeded(currentIndex: 1)
        #expect(viewModel.lazyPager.loadedItems.map(\.gamblerId) == ["a", "b", "c"])

        // Totals converged: "c" now leads.
        repository.rankedResponder = { _ in F.page(["c", "a", "b"]) }
        await viewModel.refresh()

        #expect(viewModel.lazyPager.loadedItems.map(\.gamblerId) == ["c", "a", "b"])
        #expect(repository.rankedRequests == [nil, "c1", nil])
    }

    @Test
    func aRejectedContinuationRestartsOnceAndRetryRestartsAgain() async throws {
        let repository = ScriptedMatchRepository()
        repository.betResponses[F.viewerId] = .success(F.domain(F.match(locked: true, computed: true)))
        repository.rankedResponder = { next in
            next == nil ? F.page(["a", "b"], next: "stale") : .failure(RankedMatchBetsError.cursorRejected)
        }
        let viewModel = makeViewModel(repository)
        await viewModel.load()
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 1)
        try await waitUntil { repository.rankedRequests.count == 3 }
        try await waitUntil { !viewModel.lazyPager.loadState.refresh.isLoading }
        // Automatic restart: first page, rejected continuation, first page again.
        #expect(repository.rankedRequests == [nil, "stale", nil])

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 1)
        // The second rejection in a row is shown with a retry instead of restarting again.
        guard case .failure(let error) = viewModel.lazyPager.loadState.append else {
            Issue.record("Expected an append failure")
            return
        }
        #expect(error.restartsMatchBetsFromFirstPage)
        #expect(!error.isMatchBetsReloadInProgress)
        #expect(repository.rankedRequests == [nil, "stale", nil, "stale"])

        await viewModel.retryPage(after: error)
        #expect(repository.rankedRequests == [nil, "stale", nil, "stale", nil])
        #expect(viewModel.lazyPager.loadedItems.map(\.gamblerId) == ["a", "b"])
    }

    // MARK: Fixtures

    private func makeViewModel(
        _ repository: ScriptedMatchRepository,
        routedGamblerId: String = MatchFixtures.viewerId
    ) -> MatchBetListViewModel {
        MatchBetListViewModel(
            getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase(poolGamblerBetRepository: repository),
            getRankedPoolMatchGamblerBetsUseCase: GetRankedPoolMatchGamblerBetsUseCase(
                poolGamblerBetRepository: repository
            ),
            poolId: "pool",
            gamblerId: routedGamblerId,
            matchId: "match",
            viewerAccountId: MatchFixtures.viewerId
        )
    }

    private func waitUntil(polls: Int = 500, _ condition: () -> Bool) async throws {
        var remaining = polls
        while !condition() {
            guard remaining > 0 else { throw ScriptedMatchRepository.RequestTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }
}
