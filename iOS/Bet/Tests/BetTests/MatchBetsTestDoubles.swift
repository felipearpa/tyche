import Core
import DataBet
import Foundation
@testable import Bet

/// A match-screen repository whose answers the test scripts. Single-bet requests can be held until
/// the test releases them, so overlapping requests can complete in any order.
@MainActor
final class ScriptedMatchRepository: PoolGamblerBetRepository {
    /// Gambler IDs of single-bet requests, in request order.
    private(set) var betRequests: [String] = []
    /// Cursors of ranked page requests, in request order; `nil` is a first page.
    private(set) var rankedRequests: [String?] = []

    var betResponses: [String: Result<PoolGamblerBet, Error>] = [:]
    var rankedResponder: (String?) -> Result<CursorPage<PoolGamblerBet>, Error> = { _ in
        .success(CursorPage(items: [], next: nil))
    }
    var holdsBetRequests = false
    private var heldBetRequests: [(gamblerId: String, continuation: CheckedContinuation<Void, Never>)] = []

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    func waitForHeldBetRequests(_ count: Int, polls: Int = 500) async throws {
        var remaining = polls
        while heldBetRequests.count < count {
            guard remaining > 0 else { throw RequestTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }

    /// Releases the held request at `index` (in request order among those still held).
    func releaseHeldBetRequest(at index: Int) {
        heldBetRequests.remove(at: index).continuation.resume()
    }

    private func respondToBet(gamblerId: String) async -> Result<PoolGamblerBet, Error> {
        betRequests.append(gamblerId)
        if holdsBetRequests {
            await withCheckedContinuation { continuation in
                heldBetRequests.append((gamblerId, continuation))
            }
        }
        return betResponses[gamblerId] ?? .failure(URLError(.notConnectedToInternet))
    }

    private func respondToRanked(next: String?) -> Result<CursorPage<PoolGamblerBet>, Error> {
        rankedRequests.append(next)
        return rankedResponder(next)
    }

    nonisolated func getPoolGamblerBet(
        poolId: String, gamblerId: String, matchId: String
    ) async -> Result<PoolGamblerBet, Error> {
        await respondToBet(gamblerId: gamblerId)
    }

    nonisolated func getRankedPoolMatchGamblerBets(
        poolId: String, matchId: String, next: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        await respondToRanked(next: next)
    }

    nonisolated func getPendingPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getFinishedPoolGamblerBets(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error> { .failure(Unused()) }

    nonisolated func getLivePoolGamblerBets(
        poolId: String, gamblerId: String, next: String?, searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> { .failure(Unused()) }

    nonisolated func getGamblerBetsTimeline(
        poolId: String, gamblerId: String, next: String?
    ) async -> Result<CursorPage<KickoffGroup>, Error> { .failure(Unused()) }

    nonisolated func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> { .failure(Unused()) }

    private struct Unused: Error {}
    struct RequestTimeout: Error {}
}

/// Match-screen fixtures. The repository returns the domain type, whose memberwise initializer is
/// internal to `DataBet`; both types share their Codable shape.
enum MatchFixtures {
    static let viewerId = "viewer"
    static let otherId = "other"

    static func match(
        gamblerId: String = viewerId,
        locked: Bool,
        computed: Bool = false,
        matchScore: TeamScore<Int>? = nil,
        bet: TeamScore<Int>? = TeamScore(homeTeamValue: 3, awayTeamValue: 2),
        award: Int? = nil
    ) -> PoolGamblerBetModel {
        poolGamblerBetDummyModel().copy {
            $0.poolId = "pool"
            $0.matchId = "match"
            $0.gamblerId = gamblerId
            $0.gamblerUsername = gamblerId
            $0.homeTeamName = "España"
            $0.awayTeamName = "Argentina"
            $0.isComputed = computed
            $0.matchScore = matchScore
            $0.betScore = bet
            $0.score = award
            if locked { $0.lock() }
        }
    }

    static func domain(_ model: PoolGamblerBetModel) -> PoolGamblerBet {
        try! JSONDecoder().decode(PoolGamblerBet.self, from: JSONEncoder().encode(model))
    }

    static func participant(_ gamblerId: String, award: Int? = 0) -> PoolGamblerBet {
        domain(match(gamblerId: gamblerId, locked: true, computed: true, matchScore: TeamScore(homeTeamValue: 0, awayTeamValue: 0), award: award))
    }

    static func page(_ ids: [String], next: String? = nil) -> Result<CursorPage<PoolGamblerBet>, Error> {
        .success(CursorPage(items: ids.map { participant($0) }, next: next))
    }
}
