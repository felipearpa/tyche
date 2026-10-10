import Core
import DataBet
import Foundation
import UI

/// The paging rules of one ranked participant feed. The server's order is final, so pages are
/// never sorted here. The feed:
///
/// - starts a new generation on every first-page request, discarding the participants seen so
///   far; a response that arrives after a newer generation started is rejected as superseded;
/// - drops participants already shown in this generation, because totals that change while the
///   viewer pages can move a participant across a page boundary;
/// - fetches the next page at once when deduplication leaves a page empty, a bounded number of
///   times;
/// - reports a rejected continuation and, once until an append succeeds or the viewer refreshes,
///   asks for an automatic restart from the first page;
/// - reports a hidden-predictions answer to its owner instead of showing it as a failure.
@MainActor
final class RankedMatchBetsFeed {
    typealias PageQuery = (_ next: String?) async -> Result<CursorPage<PoolGamblerBetModel>, Error>

    private let query: PageQuery
    private let onPredictionsHidden: () -> Void
    private let onRestartRequested: () -> Void

    private(set) var generation = 0
    private var seenGamblerIds: Set<String> = []
    private var automaticRestartAvailable = true
    private var inFlight: Task<Result<CursorPage<PoolGamblerBetModel>, Error>, Never>?

    init(
        query: @escaping PageQuery,
        onPredictionsHidden: @escaping () -> Void,
        onRestartRequested: @escaping () -> Void
    ) {
        self.query = query
        self.onPredictionsHidden = onPredictionsHidden
        self.onRestartRequested = onRestartRequested
    }

    /// Loads the page after `next`, or the first page of a new generation when `next` is `nil`.
    func load(next: String?) async -> Result<CursorPage<PoolGamblerBetModel>, Error> {
        if next == nil {
            generation += 1
            seenGamblerIds = []
        }
        let task = Task { await loadPage(next: next, generation: generation) }
        inFlight = task
        return await task.value
    }

    /// Waits for the page request in flight, so a refresh never overlaps an append.
    func waitUntilIdle() async {
        _ = await inFlight?.value
    }

    /// A refresh the viewer asked for makes an automatic restart available again.
    func resetAutomaticRestart() {
        automaticRestartAvailable = true
    }

    private func loadPage(next: String?, generation: Int) async -> Result<CursorPage<PoolGamblerBetModel>, Error> {
        var cursor = next
        for _ in 0...maximumEmptyPageRefills {
            let result = await query(cursor)
            guard generation == self.generation else {
                return .failure(MatchBetsFeedError.superseded)
            }
            switch result {
            case .failure(let error):
                return .failure(failure(error, wasAppend: next != nil))
            case .success(let page):
                if next != nil {
                    automaticRestartAvailable = true
                }
                let unseen = page.items.filter { seenGamblerIds.insert($0.gamblerId).inserted }
                guard unseen.isEmpty, let following = page.next else {
                    return .success(CursorPage(items: unseen, next: page.next))
                }
                cursor = following
            }
        }
        return .success(CursorPage(items: [], next: cursor))
    }

    private func failure(_ error: Error, wasAppend: Bool) -> Error {
        switch error as? RankedMatchBetsError {
        case .predictionsHidden:
            onPredictionsHidden()
            return error
        case .cursorRejected where wasAppend:
            let restarting = automaticRestartAvailable
            automaticRestartAvailable = false
            if restarting {
                onRestartRequested()
            }
            return MatchBetsFeedError.cursorRejected(restarting: restarting)
        case .rankingUnavailable:
            return MatchBetsLocalizedError.rankingUnavailable
        case .cursorRejected, nil:
            if let networkError = error as? NetworkError {
                return networkError.toNetworkLocalizedError()
            }
            return UnknownLocalizedError()
        }
    }
}

/// Outcomes of the ranked feed that the list presents itself.
enum MatchBetsFeedError: Error, Equatable {
    /// A newer generation started while this page was loading; the newer one replaces it.
    case superseded
    /// The server rejected the continuation. When `restarting`, the feed is already reloading
    /// from the first page; otherwise the viewer restarts it.
    case cursorRejected(restarting: Bool)
}

/// How many times one page request continues past a page that deduplication left empty.
private let maximumEmptyPageRefills = 3
