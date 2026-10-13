import Core
import DataBet
import Foundation
import UI

/// The paging rules shared by History and Timeline. The server sends whole kickoff groups in
/// their final order, so pages are appended as they arrive: nothing is merged, sorted, recounted,
/// or derived from the loaded rows. The feed:
///
/// - starts a new generation on every first-page request; a response that arrives after a newer
///   generation started is rejected as superseded, so it never merges into the newer list;
/// - shows each kickoff once per generation: a group whose kickoff an earlier page of the same
///   generation already showed is dropped, as when a Timeline group settles between pages and
///   comes back in the finished phase. Its newer copy appears on the next refresh. When dropping
///   leaves a page empty, the next page is fetched at once, a bounded number of times;
/// - reports a rejected continuation and, once until an append succeeds or the viewer refreshes
///   or retries, asks for an automatic restart from the first page. A second rejection in a row
///   is shown with a retry instead of restarting again.
@MainActor
final class KickoffGroupFeed {
    typealias PageQuery = (_ next: String?) async -> Result<CursorPage<KickoffGroup>, Error>

    private let query: PageQuery
    private let onRestartRequested: () -> Void

    private(set) var generation = 0
    private var seenKickoffs: Set<Date> = []
    private var automaticRestartAvailable = true
    private var inFlight: Task<Result<CursorPage<KickoffGroup>, Error>, Never>?

    init(query: @escaping PageQuery, onRestartRequested: @escaping () -> Void) {
        self.query = query
        self.onRestartRequested = onRestartRequested
    }

    /// Loads the page after `next`, or the first page of a new generation when `next` is `nil`.
    func load(next: String?) async -> Result<CursorPage<KickoffGroupModel>, Error> {
        if next == nil {
            generation += 1
            seenKickoffs = []
        }
        let requestGeneration = generation
        var cursor = next
        for _ in 0...maximumEmptyPageRefills {
            let result = await request(cursor)
            guard requestGeneration == generation else {
                return .failure(KickoffGroupFeedLoadError.superseded)
            }
            switch result {
            case .failure(let error):
                return .failure(failure(error, wasAppend: next != nil))
            case .success(let page):
                if next != nil {
                    automaticRestartAvailable = true
                }
                let unseen = page.items.filter { seenKickoffs.insert($0.kickoffDateTime).inserted }
                guard unseen.isEmpty, let following = page.next else {
                    return .success(CursorPage(items: unseen.map { $0.toKickoffGroupModel() }, next: page.next))
                }
                cursor = following
            }
        }
        return .success(CursorPage(items: [], next: cursor))
    }

    private func request(_ next: String?) async -> Result<CursorPage<KickoffGroup>, Error> {
        let task = Task { await query(next) }
        inFlight = task
        return await task.value
    }

    /// Waits for the page request in flight, so a restart never overlaps an append.
    func waitUntilIdle() async {
        _ = await inFlight?.value
    }

    /// A refresh or retry the viewer asked for makes an automatic restart available again.
    func resetAutomaticRestart() {
        automaticRestartAvailable = true
    }

    private func failure(_ error: Error, wasAppend: Bool) -> Error {
        switch error as? KickoffGroupFeedError {
        case .cursorRejected where wasAppend:
            let restarting = automaticRestartAvailable
            automaticRestartAvailable = false
            if restarting {
                onRestartRequested()
            }
            return KickoffGroupFeedLoadError.cursorRejected(restarting: restarting)
        case .cursorRejected, nil:
            if let networkError = error as? NetworkError {
                return networkError.toNetworkLocalizedError()
            }
            return UnknownLocalizedError()
        }
    }
}

/// How many times one page request continues past a page that dropping repeated kickoffs left
/// empty.
private let maximumEmptyPageRefills = 3

/// Outcomes of a grouped feed page that the lists present themselves.
enum KickoffGroupFeedLoadError: Error, Equatable {
    /// A newer generation started while this page was loading; the newer one replaces it.
    case superseded
    /// The server rejected the continuation. When `restarting`, the feed is already reloading from
    /// the first page; otherwise the viewer restarts it.
    case cursorRejected(restarting: Bool)
}

/// The list's message when the server rejects a continuation again right after a restart.
enum KickoffGroupFeedLocalizedError: LocalizedError, Equatable {
    case listChanged

    var errorDescription: String? {
        String(localized: .historyListChangedTitle)
    }

    var failureReason: String? {
        String(localized: .historyListChangedMessage)
    }
}

extension Error {
    /// The error a grouped list shows for a failed page.
    func kickoffGroupFeedLocalized() -> LocalizedError {
        if case .cursorRejected = self as? KickoffGroupFeedLoadError {
            return KickoffGroupFeedLocalizedError.listChanged
        }
        return orDefaultLocalized()
    }

    /// Whether the list's retry must start again from the first page instead of repeating the
    /// failed continuation.
    var restartsKickoffGroupFeed: Bool {
        if case .cursorRejected = self as? KickoffGroupFeedLoadError { return true }
        return false
    }

    /// Whether the failed page is already being replaced, so the list keeps showing loading
    /// instead of an error.
    var isKickoffGroupReloadInProgress: Bool {
        switch self as? KickoffGroupFeedLoadError {
        case .superseded, .cursorRejected(restarting: true): true
        default: false
        }
    }
}
