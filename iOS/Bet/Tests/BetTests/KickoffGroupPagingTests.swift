import Core
import DataBet
import DataPool
import Foundation
import Testing
import UI
@testable import Bet

/// Group-level paging shared by History and Timeline: whole groups appended in server order, a
/// superseded response never merged into a newer list, and a rejected continuation restarted from
/// the first page once before the list offers its retry. Real data cannot stage a rejected
/// continuation or out-of-order responses, so these tests cover them with a scripted feed.
@MainActor
struct KickoffGroupPagingTests {
    @Test
    func historyAppendsWholeGroupsInServerOrderWithTheirServerCountsAndMovement() async throws {
        let feed = ScriptedGroupedFeedRepository(pages: [
            GroupFixtures.page([
                GroupFixtures.group("a", kickoff: GroupFixtures.kickoff(hours: 0), total: 4, computed: 2, rows: 2, movement: (8, 6)),
            ], next: "page-2"),
            GroupFixtures.page([
                GroupFixtures.group("b", kickoff: GroupFixtures.kickoff(hours: -3), total: 1, computed: 1, movement: (4, 5)),
                GroupFixtures.group("c", kickoff: GroupFixtures.kickoff(hours: -5), total: 3, computed: 3, movement: nil),
            ]),
        ])
        let viewModel = makeHistory(feed)

        await viewModel.lazyPager.refresh()
        await viewModel.lazyPager.onRowAccess(index: 0)

        let groups = viewModel.lazyPager.loadedItems
        #expect(feed.requests.map(\.next) == [nil, "page-2"])
        #expect(groups.map(\.kickoffDateTime) == [0, -3, -5].map { GroupFixtures.kickoff(hours: $0) })
        #expect(groups.map(\.totalMatches) == [4, 1, 3])
        #expect(groups.map(\.computedMatches) == [2, 1, 3])
        #expect(groups.map(\.movement) == [
            .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 2, totalMatches: 4)),
            .moved(places: -1, partialProgress: nil),
            .unavailable,
        ])
        #expect(groups[0].matches.map(\.matchId) == ["a-0", "a-1"])
    }

    @Test
    func aRefreshDuringAnAppendLeavesOnlyTheRefreshedGroups() async throws {
        let feed = ScriptedGroupedFeedRepository(pages: [
            GroupFixtures.page([GroupFixtures.group("old", kickoff: GroupFixtures.kickoff(hours: 0))], next: "page-2"),
            GroupFixtures.page([GroupFixtures.group("stale", kickoff: GroupFixtures.kickoff(hours: -1))]),
        ])
        let viewModel = makeHistory(feed)
        await viewModel.lazyPager.refresh()

        feed.holdsRequests = true
        let append = Task { await viewModel.lazyPager.onRowAccess(index: 0) }
        try await feed.waitForHeldRequests(1)

        feed.pages = [GroupFixtures.page([GroupFixtures.group("new", kickoff: GroupFixtures.kickoff(hours: 2))])]
        let refresh = Task { await viewModel.refreshListAndPointsSummary() }
        // The refresh waits for the append in flight, so release the append first, then the
        // refresh's own first page.
        feed.releaseHeldRequest()
        await append.value
        try await feed.waitForHeldRequests(1)
        feed.releaseHeldRequest()
        await refresh.value

        #expect(viewModel.lazyPager.loadedItems.map { $0.matches.map(\.matchId) } == [["new-0", "new-1"]])
    }

    @Test
    func aStalePageFromAnEarlierGenerationIsRejectedAsSuperseded() async throws {
        let repository = ScriptedGroupedFeedRepository(pages: [GroupFixtures.page([GroupFixtures.group("x")])])
        repository.holdsRequests = true
        let feed = KickoffGroupFeed(
            query: { next in await repository.getFinishedPoolGamblerBets(poolId: "p", gamblerId: "g", next: next) },
            onRestartRequested: {}
        )

        let stale = Task { await feed.load(next: "page-2") }
        try await repository.waitForHeldRequests(1)
        let fresh = Task { await feed.load(next: nil) }
        try await repository.waitForHeldRequests(2)
        repository.releaseHeldRequest(at: 1)
        _ = try await fresh.value.get()
        repository.releaseHeldRequest(at: 0)

        guard case .failure(let error) = await stale.value else {
            Issue.record("Expected the stale page to be rejected")
            return
        }
        #expect(error as? KickoffGroupFeedLoadError == .superseded)
        #expect(error.isKickoffGroupReloadInProgress)
    }

    @Test
    func aRejectedContinuationReloadsTheFirstPageOnceWhileTheGroupsStayVisible() async throws {
        let feed = ScriptedGroupedFeedRepository()
        feed.responder = { next in
            next == nil
                ? .success(GroupFixtures.page([GroupFixtures.group("fresh")], next: "page-2"))
                : .failure(KickoffGroupFeedError.cursorRejected)
        }
        let viewModel = makeHistory(feed)
        await viewModel.lazyPager.refresh()

        feed.holdsRequests = true
        let append = Task { await viewModel.lazyPager.onRowAccess(index: 0) }
        try await feed.waitForHeldRequests(1)
        feed.releaseHeldRequest()
        await append.value

        // The automatic restart is under way: the loaded group is still shown, and the failed
        // append presents as loading rather than as an error.
        try await feed.waitForHeldRequests(1)
        #expect(viewModel.lazyPager.itemCount == 1)
        #expect(viewModel.lazyPager.loadState.refresh.isLoading)
        #expect(appendFailure(viewModel).map { $0.isKickoffGroupReloadInProgress } ?? true)

        feed.holdsRequests = false
        feed.releaseHeldRequest()
        try await waitUntil { !viewModel.lazyPager.loadState.refresh.isLoading }

        #expect(feed.requests.map(\.next) == [nil, "page-2", nil])
        #expect(viewModel.lazyPager.loadedItems.map { $0.matches.map(\.matchId) } == [["fresh-0", "fresh-1"]])
    }

    @Test
    func aSecondRejectionInARowShowsTheRetryInsteadOfRestartingAgain() async throws {
        let feed = ScriptedGroupedFeedRepository()
        feed.responder = { next in
            next == nil
                ? .success(GroupFixtures.page([GroupFixtures.group("fresh")], next: "page-2"))
                : .failure(KickoffGroupFeedError.cursorRejected)
        }
        let viewModel = makeHistory(feed)
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.onRowAccess(index: 0)
        try await waitUntil { feed.requestCount == 3 && !viewModel.lazyPager.loadState.refresh.isLoading }
        await viewModel.lazyPager.onRowAccess(index: 0)
        // Give an unwanted automatic restart the chance to start before checking it did not.
        try await Task.sleep(for: .milliseconds(100))

        #expect(feed.requests.map(\.next) == [nil, "page-2", nil, "page-2"])
        let error = try #require(appendFailure(viewModel))
        #expect(error as? KickoffGroupFeedLoadError == .cursorRejected(restarting: false))
        #expect(!error.isKickoffGroupReloadInProgress)
        #expect(error.kickoffGroupFeedLocalized() as? KickoffGroupFeedLocalizedError == .listChanged)
        #expect(viewModel.lazyPager.itemCount == 1)

        // The list's retry starts again from the first page.
        await viewModel.retryPage(after: error)
        #expect(feed.requests.map(\.next) == [nil, "page-2", nil, "page-2", nil])
    }

    @Test
    func timelineRestartsARejectedContinuationTheSameWay() async throws {
        let feed = ScriptedGroupedFeedRepository()
        feed.responder = { next in
            next == nil
                ? .success(GroupFixtures.page([GroupFixtures.group("unsettled", computed: 0, movement: nil)], next: "live:x"))
                : .failure(KickoffGroupFeedError.cursorRejected)
        }
        let viewModel = BetTimelineListViewModel(
            getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(poolGamblerBetRepository: feed),
            getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: UnusedScores()),
            poolId: "pool-id",
            gamblerId: "gambler-id"
        )
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.onRowAccess(index: 0)
        try await waitUntil { feed.requestCount == 3 && !viewModel.lazyPager.loadState.refresh.isLoading }

        #expect(feed.requests.map(\.next) == [nil, "live:x", nil])
        #expect(viewModel.lazyPager.loadedItems.map(\.movement) == [.pending])
    }

    @Test
    func anOrdinaryAppendFailureRetriesTheSameContinuation() async throws {
        let feed = ScriptedGroupedFeedRepository()
        var failsAppend = true
        // One group: its row is both the last row and a top row. History and Timeline show rows
        // through `AppendPagingVStack`, whose rows only call `appendIfNeeded`, so the failed append
        // stays the load that Retry repeats.
        let firstPage = GroupFixtures.page([GroupFixtures.group("a")], next: "page-2")
        feed.responder = { next in
            if next == nil { return .success(firstPage) }
            if failsAppend { return .failure(URLError(.timedOut)) }
            return .success(GroupFixtures.page([GroupFixtures.group("b", kickoff: GroupFixtures.kickoff(hours: -9))]))
        }
        let viewModel = makeHistory(feed)
        await viewModel.lazyPager.refresh()
        await viewModel.lazyPager.appendIfNeeded(currentIndex: 0)
        let error = try #require(appendFailure(viewModel))
        #expect(!error.restartsKickoffGroupFeed)

        failsAppend = false
        await viewModel.retryPage(after: error)

        #expect(feed.requests.map(\.next) == [nil, "page-2", "page-2"])
        #expect(viewModel.lazyPager.itemCount == 2)
    }

    @Test
    func aTimelineGroupThatSettlesBetweenPagesAppearsOnceAndItsSettledCopyAfterARefresh() async throws {
        let kickoff = GroupFixtures.kickoff(hours: 0)
        let unsettled = GroupFixtures.group("g", kickoff: kickoff, total: 2, computed: 1, movement: (8, 6))
        let settled = GroupFixtures.group("g", kickoff: kickoff, total: 2, computed: 2, movement: (8, 5))
        let older = GroupFixtures.group("h", kickoff: GroupFixtures.kickoff(hours: -5))
        let feed = ScriptedGroupedFeedRepository(pages: [
            GroupFixtures.page([unsettled], next: "finished:1"),
            // The group settled after the first page: the finished phase returns it again.
            GroupFixtures.page([settled, older]),
        ])
        let viewModel = makeTimeline(feed)
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 0)

        let shown = viewModel.lazyPager.loadedItems
        #expect(shown.map(\.kickoffDateTime) == [kickoff, GroupFixtures.kickoff(hours: -5)])
        #expect(Set(shown.map(\.id)).count == shown.count)
        #expect(shown[0].movement == .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2)))

        feed.pages = [GroupFixtures.page([settled, older])]
        await viewModel.retryFirstPage()

        #expect(viewModel.lazyPager.loadedItems.map(\.kickoffDateTime) == [kickoff, GroupFixtures.kickoff(hours: -5)])
        #expect(viewModel.lazyPager.loadedItems[0].movement == .moved(places: 3, partialProgress: nil))
    }

    @Test
    func aPageLeftEmptyByARepeatedGroupContinuesToTheNextPage() async throws {
        let kickoff = GroupFixtures.kickoff(hours: 0)
        let feed = ScriptedGroupedFeedRepository(pages: [
            GroupFixtures.page([GroupFixtures.group("g", kickoff: kickoff, computed: 1, movement: (8, 6))], next: "finished:1"),
            GroupFixtures.page([GroupFixtures.group("g", kickoff: kickoff)], next: "finished:2"),
            GroupFixtures.page([GroupFixtures.group("h", kickoff: GroupFixtures.kickoff(hours: -5))]),
        ])
        let viewModel = makeTimeline(feed)
        await viewModel.lazyPager.refresh()

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 0)

        #expect(feed.requests.map(\.next) == [nil, "finished:1", "finished:2"])
        #expect(viewModel.lazyPager.loadedItems.map(\.kickoffDateTime) == [kickoff, GroupFixtures.kickoff(hours: -5)])
    }

    // MARK: Helpers

    private func makeHistory(_ feed: ScriptedGroupedFeedRepository) -> FinishedBetListViewModel {
        FinishedBetListViewModel(
            getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(poolGamblerBetRepository: feed),
            getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: UnusedScores()),
            gamblerId: "gambler-id",
            poolId: "pool-id"
        )
    }

    private func makeTimeline(_ feed: ScriptedGroupedFeedRepository) -> BetTimelineListViewModel {
        BetTimelineListViewModel(
            getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase(poolGamblerBetRepository: feed),
            getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: UnusedScores()),
            poolId: "pool-id",
            gamblerId: "gambler-id"
        )
    }

    private func appendFailure(_ viewModel: FinishedBetListViewModel) -> Error? {
        if case .failure(let error) = viewModel.lazyPager.loadState.append { return error }
        return nil
    }

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    private func waitUntil(polls: Int = 500, _ condition: () -> Bool) async throws {
        var remaining = polls
        while !condition() {
            guard remaining > 0 else { throw ScriptedGroupedFeedRepository.FeedTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }
}

/// Paging tests never load the total; a request would fail the test through its unusable result.
private final class UnusedScores: PoolGamblerScoreRepository {
    func getPoolGamblerScore(poolId: String, gamblerId: String) async -> Result<PoolGamblerScore, Error> {
        .failure(Unused())
    }

    func getPoolGamblerScoresByGambler(
        gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> { .failure(Unused()) }

    func getPoolGamblerScoresByPool(
        poolId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> { .failure(Unused()) }

    private struct Unused: Error {}
}
