import Foundation
import UI

/// Recoverable failures specific to the match screen's ranked participant list.
enum MatchBetsLocalizedError: LocalizedError, Equatable {
    /// The pool's ranking is not ready or could not be read. There is no other order to show.
    case rankingUnavailable
    /// The server no longer accepts the list's continuation, so it must load from the start.
    case listChanged

    var errorDescription: String? {
        switch self {
        case .rankingUnavailable: String(localized: .matchBetsRankingUnavailableTitle)
        case .listChanged: String(localized: .matchBetsListChangedTitle)
        }
    }

    var failureReason: String? {
        switch self {
        case .rankingUnavailable: String(localized: .matchBetsRankingUnavailableMessage)
        case .listChanged: String(localized: .matchBetsListChangedMessage)
        }
    }
}

extension Error {
    /// The error the participant list shows for a failed page.
    func matchBetsLocalized() -> LocalizedError {
        if case .cursorRejected = self as? MatchBetsFeedError {
            return MatchBetsLocalizedError.listChanged
        }
        return orDefaultLocalized()
    }

    /// Whether the list's retry must start again from the first page instead of repeating the
    /// failed continuation.
    var restartsMatchBetsFromFirstPage: Bool {
        if case .cursorRejected = self as? MatchBetsFeedError { return true }
        return false
    }

    /// Whether the failed page is already being replaced, so the list shows loading instead of
    /// an error.
    var isMatchBetsReloadInProgress: Bool {
        switch self as? MatchBetsFeedError {
        case .superseded, .cursorRejected(restarting: true): true
        default: false
        }
    }
}
