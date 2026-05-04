import Testing
import Core
@testable import DataBet

@Suite("PoolGamblerBetRemoteRepository")
struct PoolGamblerBetRemoteRepositoryTests {

    @Test("given a 403 from the network when bet is called then result fails with BetError.forbidden")
    func forbiddenMapsToBetErrorForbidden() async {
        let repository = makeRepository(
            handlerResult: .failure(NetworkError.http(code: .forbidden))
        )

        let result = await repository.bet(bet: makeBet())

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            #expect(error is BetError)
            if case BetError.forbidden = error {
            } else {
                Issue.record("Expected BetError.forbidden, got \(error)")
            }
        }
    }

    @Test("given a non-403 http error when bet is called then result fails with NetworkError.http preserved")
    func nonForbiddenHttpStaysAsNetworkError() async {
        let repository = makeRepository(
            handlerResult: .failure(NetworkError.http(code: .internalServerError))
        )

        let result = await repository.bet(bet: makeBet())

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            guard let networkError = error as? NetworkError else {
                Issue.record("Expected NetworkError, got \(error)")
                return
            }
            if case .http(let code) = networkError {
                #expect(code == .internalServerError)
            } else {
                Issue.record("Expected NetworkError.http, got \(networkError)")
            }
        }
    }

    @Test("given a non-network error when bet is called then result fails with the original error")
    func nonNetworkErrorPassesThrough() async {
        let repository = makeRepository(
            handlerResult: .failure(StubError.boom)
        )

        let result = await repository.bet(bet: makeBet())

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            #expect(error is StubError)
        }
    }

    private func makeRepository(handlerResult: Result<PoolGamblerBet, Error>) -> PoolGamblerBetRemoteRepository {
        PoolGamblerBetRemoteRepository(
            poolGamblerBetRemoteDataSource: UnusedDataSource(),
            networkErrorHandler: StubNetworkErrorHandler(result: handlerResult)
        )
    }

    private func makeBet() -> Bet {
        Bet(
            poolId: "01K1PX1TX2NM1HG851S1V0QG6N",
            gamblerId: "01K1PX1TX2NM1HG851S1V0QG6P",
            matchId: "01K1PX1TX2NM1HG851S1V0QG6Q",
            homeTeamBet: BetScore(1)!,
            awayTeamBet: BetScore(0)!
        )
    }
}

private enum StubError: Error {
    case boom
}

private final class StubNetworkErrorHandler: NetworkErrorHandler {
    private let result: Result<PoolGamblerBet, Error>

    init(result: Result<PoolGamblerBet, Error>) {
        self.result = result
    }

    func handle<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
        switch result {
        case .success(let value):
            return .success(value as! Value)
        case .failure(let error):
            return .failure(error)
        }
    }
}

private final class UnusedDataSource: PoolGamblerBetRemoteDataSource {
    func getPoolGamblerBet(poolId: String, gamblerId: String, matchId: String) async throws -> DataBet.PoolGamblerBetResponse {
        fatalError("not used")
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

    func getPoolMatchGamblerBets(poolId: String, matchId: String, next: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getGamblerBetsTimeline(poolId: String, gamblerId: String, next: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse {
        fatalError("not used")
    }
}
