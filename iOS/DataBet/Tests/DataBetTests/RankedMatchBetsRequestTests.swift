import Core
import Foundation
import Testing
@testable import DataBet

/// The ranked match-bets request: its query and how its error responses are classified.
@Suite("Ranked match bets request")
struct RankedMatchBetsRequestTests {
    @Test("the first page sends only the explicit sort, never an empty or placeholder cursor")
    func firstPageSendsOnlyTheSort() {
        #expect(rankedMatchBetsParameters(next: nil) == ["sort": "pool-score-desc"])
    }

    @Test("a continuation is sent with the sort")
    func continuationIsSentWithTheSort() {
        #expect(rankedMatchBetsParameters(next: "abc") == ["sort": "pool-score-desc", "next": "abc"])
    }

    @Test(
        "each documented error code maps to its ranked outcome",
        arguments: [
            (409, "PREDICTIONS_HIDDEN", RankedMatchBetsError.predictionsHidden),
            (503, "RANKING_NOT_READY", .rankingUnavailable),
            (503, "RANKING_UNAVAILABLE", .rankingUnavailable),
            (400, "INVALID_CURSOR", .cursorRejected),
            (400, "CURSOR_CONTEXT_MISMATCH", .cursorRejected),
        ]
    )
    func documentedCodesMap(statusCode: Int, code: String, expected: RankedMatchBetsError) {
        #expect(rankedMatchBetsError(statusCode: statusCode, body: body(code)) == expected)
    }

    @Test(
        "authentication, membership, not-found, and unknown responses keep the transport error",
        arguments: [
            (401, nil as String?),
            (403, "NOT_POOL_MEMBER"),
            (404, "MATCH_NOT_FOUND"),
            (409, "SOMETHING_ELSE"),
            (500, nil),
        ]
    )
    func otherResponsesAreNotRankedErrors(statusCode: Int, code: String?) {
        #expect(rankedMatchBetsError(statusCode: statusCode, body: code.map(body)) == nil)
    }

    @Test("a body without the error field is not a ranked error")
    func malformedBodyIsNotARankedError() {
        #expect(rankedMatchBetsError(statusCode: 503, body: Data("<html>".utf8)) == nil)
        #expect(rankedMatchBetsError(statusCode: nil, body: body("RANKING_NOT_READY")) == nil)
    }

    @Test("the repository passes a ranked error through unchanged")
    func repositoryPassesRankedErrorsThrough() async {
        let repository = PoolGamblerBetRemoteRepository(
            poolGamblerBetRemoteDataSource: FailingRankedDataSource(error: RankedMatchBetsError.predictionsHidden),
            networkErrorHandler: PassThroughNetworkErrorHandler()
        )

        let result = await repository.getRankedPoolMatchGamblerBets(poolId: "pool", matchId: "match", next: nil)

        guard case .failure(let error) = result else {
            Issue.record("Expected a failure")
            return
        }
        #expect(error as? RankedMatchBetsError == .predictionsHidden)
    }

    private func body(_ code: String) -> Data {
        Data(#"{"error":"\#(code)","message":"text"}"#.utf8)
    }
}

private struct PassThroughNetworkErrorHandler: NetworkErrorHandler {
    func handle<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
        do {
            return .success(try await perform())
        } catch {
            return .failure(error)
        }
    }
}

private final class FailingRankedDataSource: PoolGamblerBetRemoteDataSource {
    let error: Error

    init(error: Error) {
        self.error = error
    }

    func getRankedPoolMatchGamblerBets(poolId: String, matchId: String, next: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        throw error
    }

    func getPendingPoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getFinishedPoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getLivePoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getPoolGamblerBet(poolId: String, gamblerId: String, matchId: String) async throws -> PoolGamblerBetResponse {
        fatalError("not used")
    }

    func getGamblerBetsTimeline(poolId: String, gamblerId: String, next: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse {
        fatalError("not used")
    }
}
