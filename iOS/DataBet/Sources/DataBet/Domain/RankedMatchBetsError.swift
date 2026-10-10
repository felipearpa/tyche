/// Failures of the ranked match-bets request that callers handle differently from an ordinary
/// network failure. Authentication, membership, and not-found responses keep the repository's
/// usual `NetworkError`.
public enum RankedMatchBetsError: Error, Equatable {
    /// Betting is still open for the match, so other participants' bets stay hidden (409
    /// `PREDICTIONS_HIDDEN`). This is a state of the match, not a failure to show.
    case predictionsHidden
    /// The pool's ranking is not ready or could not be read (503 `RANKING_NOT_READY` or
    /// `RANKING_UNAVAILABLE`). Recoverable by retrying later; there is no alternative order.
    case rankingUnavailable
    /// The server rejected the continuation (400 `INVALID_CURSOR` or
    /// `CURSOR_CONTEXT_MISMATCH`). The feed restarts from its first page.
    case cursorRejected

    /// The ranked error for a failed response, or `nil` when the response is not one of them.
    static func from(statusCode: Int, errorCode: String?) -> RankedMatchBetsError? {
        switch (statusCode, errorCode) {
        case (409, "PREDICTIONS_HIDDEN"):
            return .predictionsHidden
        case (503, "RANKING_NOT_READY"), (503, "RANKING_UNAVAILABLE"):
            return .rankingUnavailable
        case (400, "INVALID_CURSOR"), (400, "CURSOR_CONTEXT_MISMATCH"):
            return .cursorRejected
        default:
            return nil
        }
    }
}

/// The body of a ranked match-bets error response.
struct RankedMatchBetsErrorResponse: Decodable {
    let error: String
}
