/// Failures of the grouped History and Timeline feeds that callers handle differently from an
/// ordinary network failure.
public enum KickoffGroupFeedError: Error, Equatable {
    /// The server rejected the continuation (400 `INVALID_CURSOR` or `CURSOR_CONTEXT_MISMATCH`),
    /// for example one issued before the grouped feeds. The feed restarts from its first page.
    case cursorRejected

    /// The grouped-feed error for a failed response, or `nil` when the response is not one of them.
    static func from(statusCode: Int, errorCode: String?) -> KickoffGroupFeedError? {
        switch (statusCode, errorCode) {
        case (400, "INVALID_CURSOR"), (400, "CURSOR_CONTEXT_MISMATCH"):
            return .cursorRejected
        default:
            return nil
        }
    }
}
