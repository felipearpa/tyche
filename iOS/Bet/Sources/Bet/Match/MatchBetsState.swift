import Core
import Foundation

/// What the match screen shows about other participants' bets, from the server's lock and
/// computation flags. Computation takes precedence over the lock flag. None of these states
/// claims that play is live: the lock happens before kickoff and computation can be delayed.
enum MatchBetsPresentation: Equatable, Sendable {
    /// Betting is open, so only the viewer's own bet is shown.
    case hidden
    /// Betting is closed and points are not computed: predictions without awards.
    case betsClosed
    /// Points are computed: predictions with this match's awards.
    case computed

    /// - Parameter predictionsHiddenByServer: The ranked feed answered that predictions are
    ///   hidden. The server's privacy gate wins over every flag the heading carries.
    init(match: PoolGamblerBetModel, predictionsHiddenByServer: Bool = false) {
        if predictionsHiddenByServer {
            self = .hidden
        } else if match.isComputed {
            self = .computed
        } else if match.isLocked {
            self = .betsClosed
        } else {
            self = .hidden
        }
    }

    var revealsParticipants: Bool {
        self != .hidden
    }
}

/// The match heading and its privacy gate: the last match the server confirmed and the state of
/// the latest request. They are independent, so a refresh or a failed refresh keeps the confirmed
/// heading on screen.
struct MatchHeadingState: Equatable, Sendable {
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
        /// No match confirmed yet and a request is pending: the gate is unknown.
        case placeholder
        /// No match confirmed yet and the request failed.
        case failed
        case loaded(PoolGamblerBetModel, MatchBetsPresentation, RefreshStatus)
    }

    let match: PoolGamblerBetModel?
    let request: Request
    /// Set when the ranked feed answers that predictions are hidden; cleared when a later
    /// heading load confirms betting is closed, so the feed is asked again.
    let predictionsHiddenByServer: Bool

    static let initial = MatchHeadingState(match: nil, request: .idle, predictionsHiddenByServer: false)

    var presentation: Presentation {
        guard let match else {
            return request == .failed ? .failed : .placeholder
        }
        let status: RefreshStatus = switch request {
        case .idle: .current
        case .loading: .refreshing
        case .failed: .failed
        }
        return .loaded(
            match,
            MatchBetsPresentation(match: match, predictionsHiddenByServer: predictionsHiddenByServer),
            status
        )
    }

    /// The confirmed state of other participants' bets, or `nil` while the gate is unknown.
    var confirmedPresentation: MatchBetsPresentation? {
        if case .loaded(_, let presentation, _) = presentation { return presentation }
        return nil
    }

    func loading() -> MatchHeadingState {
        MatchHeadingState(match: match, request: .loading, predictionsHiddenByServer: predictionsHiddenByServer)
    }

    /// A freshly confirmed match. It clears a server-reported hidden gate: an open match is
    /// hidden by its own flags, and a closed one asks the feed again.
    func loaded(_ match: PoolGamblerBetModel) -> MatchHeadingState {
        MatchHeadingState(match: match, request: .idle, predictionsHiddenByServer: false)
    }

    func failed() -> MatchHeadingState {
        MatchHeadingState(match: match, request: .failed, predictionsHiddenByServer: predictionsHiddenByServer)
    }

    func hiddenByServer() -> MatchHeadingState {
        MatchHeadingState(match: match, request: request, predictionsHiddenByServer: true)
    }
}

/// The signed-in viewer's own prediction for the match, shown while bets are hidden. Loading,
/// a failed request, and a confirmed absence stay distinct.
enum OwnBetState: Equatable, Sendable {
    case loading
    case bet(TeamScore<Int>)
    case noBet
    case failed

    init(betScore: TeamScore<Int>?) {
        self = betScore.map(OwnBetState.bet) ?? .noBet
    }
}
