import Foundation

/// The points the signed-in gambler has earned in the current pool, as the server reports them.
/// `unavailable` is a successful response without a total; it is never treated as zero.
enum HistoryPoints: Equatable, Sendable {
    case earned(Int)
    case unavailable

    init(score: Int?) {
        self = score.map(HistoryPoints.earned) ?? .unavailable
    }
}

/// History's earned-points summary: the last points the server confirmed and the state of the
/// latest request. The two are independent, so a refresh or a failed refresh keeps the confirmed
/// points on screen.
struct HistoryPointsSummaryState: Equatable, Sendable {
    enum Request: Equatable, Sendable {
        case idle
        case loading
        case failed
    }

    enum RefreshStatus: Equatable, Sendable {
        case current
        case refreshing
        case failed
    }

    enum Presentation: Equatable, Sendable {
        /// No points confirmed yet and a request is pending.
        case placeholder
        /// No points confirmed yet and the request failed.
        case failed
        case points(HistoryPoints, RefreshStatus)
    }

    let points: HistoryPoints?
    let request: Request

    static let initial = HistoryPointsSummaryState(points: nil, request: .idle)

    var presentation: Presentation {
        guard let points else {
            return request == .failed ? .failed : .placeholder
        }
        switch request {
        case .idle:
            return .points(points, .current)
        case .loading:
            return .points(points, .refreshing)
        case .failed:
            return .points(points, .failed)
        }
    }

    var isLoading: Bool {
        request == .loading
    }

    func loading() -> HistoryPointsSummaryState {
        HistoryPointsSummaryState(points: points, request: .loading)
    }

    func loaded(_ points: HistoryPoints) -> HistoryPointsSummaryState {
        HistoryPointsSummaryState(points: points, request: .idle)
    }

    func failed() -> HistoryPointsSummaryState {
        HistoryPointsSummaryState(points: points, request: .failed)
    }
}
