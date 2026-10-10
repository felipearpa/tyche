import Core
import DataBet
import Foundation
import Testing
@testable import Bet

/// The ranked participant feed's paging rules: server order, deduplication by gambler,
/// generations, bounded refills, and recovery from rejected continuations.
@MainActor
struct RankedMatchBetsFeedTests {
    @Test
    func pagesKeepTheServersOrderAndDropParticipantsAlreadyShown() async throws {
        let feed = makeFeed { next in next == nil ? page(["a", "b", "c"], next: "1") : page(["c", "d"]) }

        let first = try await feed.load(next: nil).get()
        let second = try await feed.load(next: "1").get()

        #expect(first.items.map(\.gamblerId) == ["a", "b", "c"])
        #expect(second.items.map(\.gamblerId) == ["d"])
    }

    @Test
    func aFirstPageStartsANewGenerationThatForgetsEarlierParticipants() async throws {
        let feed = makeFeed { _ in page(["a", "b"]) }

        _ = try await feed.load(next: nil).get()
        let refreshed = try await feed.load(next: nil).get()

        #expect(refreshed.items.map(\.gamblerId) == ["a", "b"])
        #expect(feed.generation == 2)
    }

    @Test
    func aPageFromAnEarlierGenerationIsRejected() async throws {
        let gate = Gate()
        let feed = makeFeed { next in
            if next == "1" { await gate.wait() }
            return page(["x"])
        }
        _ = try await feed.load(next: nil).get()

        let stale = Task { await feed.load(next: "1") }
        try await gate.waitUntilWaiting()
        _ = await feed.load(next: nil)
        gate.open()

        guard case .failure(let error) = await stale.value else {
            Issue.record("Expected the stale page to be rejected")
            return
        }
        #expect(error as? MatchBetsFeedError == .superseded)
        #expect(error.isMatchBetsReloadInProgress)
    }

    @Test
    func aPageEmptiedByDeduplicationContinuesToTheNextPageABoundedNumberOfTimes() async throws {
        var requests: [String?] = []
        let feed = makeFeed { next in
            requests.append(next)
            switch next {
            case nil: return page(["a", "b"], next: "1")
            case "1": return page(["b"], next: "2")
            default: return page(["c"])
            }
        }
        _ = try await feed.load(next: nil).get()

        let refilled = try await feed.load(next: "1").get()
        #expect(refilled.items.map(\.gamblerId) == ["c"])
        #expect(requests == [nil, "1", "2"])

        let endless = makeFeed { next in next == nil ? page(["a"], next: "x") : page(["a"], next: "x") }
        _ = try await endless.load(next: nil).get()
        let bounded = try await endless.load(next: "x").get()
        #expect(bounded.items.isEmpty)
        #expect(bounded.next == "x")
    }

    @Test
    func aHiddenAnswerIsReportedToTheOwner() async {
        var hidden = 0
        let feed = RankedMatchBetsFeed(
            query: { _ in .failure(RankedMatchBetsError.predictionsHidden) },
            onPredictionsHidden: { hidden += 1 },
            onRestartRequested: {}
        )

        _ = await feed.load(next: nil)

        #expect(hidden == 1)
    }

    @Test
    func anUnavailableRankingIsARecoverableErrorWithItsOwnMessage() async {
        let feed = makeFeed { _ in .failure(RankedMatchBetsError.rankingUnavailable) }

        guard case .failure(let error) = await feed.load(next: nil) else {
            Issue.record("Expected a failure")
            return
        }
        #expect(error as? MatchBetsLocalizedError == .rankingUnavailable)
        #expect(!error.restartsMatchBetsFromFirstPage)
    }

    @Test
    func aRejectedContinuationRestartsOnceUntilAnAppendSucceedsOrTheViewerRefreshes() async throws {
        var restarts = 0
        var rejectsContinuation = true
        let feed = RankedMatchBetsFeed(
            query: { next in
                if next != nil, rejectsContinuation { return .failure(RankedMatchBetsError.cursorRejected) }
                return page(next == nil ? ["a"] : ["b"], next: "1")
            },
            onPredictionsHidden: {},
            onRestartRequested: { restarts += 1 }
        )
        _ = try await feed.load(next: nil).get()

        #expect(await failure(feed.load(next: "1")) == .cursorRejected(restarting: true))
        #expect(await failure(feed.load(next: "1")) == .cursorRejected(restarting: false))
        #expect(restarts == 1)

        feed.resetAutomaticRestart()
        #expect(await failure(feed.load(next: "1")) == .cursorRejected(restarting: true))
        #expect(restarts == 2)

        rejectsContinuation = false
        _ = try await feed.load(next: "1").get()
        rejectsContinuation = true
        #expect(await failure(feed.load(next: "1")) == .cursorRejected(restarting: true))
        #expect(restarts == 3)
    }

    @Test
    func aRejectedFirstPageIsAnOrdinaryFailure() async {
        var restarts = 0
        let feed = RankedMatchBetsFeed(
            query: { _ in .failure(RankedMatchBetsError.cursorRejected) },
            onPredictionsHidden: {},
            onRestartRequested: { restarts += 1 }
        )

        guard case .failure(let error) = await feed.load(next: nil) else {
            Issue.record("Expected a failure")
            return
        }
        #expect(restarts == 0)
        #expect(!error.restartsMatchBetsFromFirstPage)
    }

    // MARK: Fixtures

    private func makeFeed(
        _ query: @escaping RankedMatchBetsFeed.PageQuery
    ) -> RankedMatchBetsFeed {
        RankedMatchBetsFeed(query: query, onPredictionsHidden: {}, onRestartRequested: {})
    }

    private func failure(_ result: Result<CursorPage<PoolGamblerBetModel>, Error>) -> MatchBetsFeedError? {
        guard case .failure(let error) = result else { return nil }
        return error as? MatchBetsFeedError
    }
}

private func page(_ ids: [String], next: String? = nil) -> Result<CursorPage<PoolGamblerBetModel>, Error> {
    .success(
        CursorPage(
            items: ids.map { id in matchGamblerBetPlaceholderModel(isComputed: true).copy { $0.gamblerId = id } },
            next: next
        )
    )
}

/// Holds a request until the test opens it.
@MainActor
private final class Gate {
    private var continuation: CheckedContinuation<Void, Never>?

    func wait() async {
        await withCheckedContinuation { continuation = $0 }
    }

    func waitUntilWaiting(polls: Int = 500) async throws {
        var remaining = polls
        while continuation == nil {
            guard remaining > 0 else { throw ScriptedMatchRepository.RequestTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    func open() {
        continuation?.resume()
        continuation = nil
    }
}
