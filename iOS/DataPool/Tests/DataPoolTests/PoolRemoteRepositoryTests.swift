import Testing
import Core
@testable import DataPool

@Suite("PoolRemoteRepository")
struct PoolRemoteRepositoryTests {

    @Test("given a 409 from the network when joinPool is called then result fails with JoinPoolError.alreadyJoined")
    func conflictMapsToJoinPoolErrorAlreadyJoined() async {
        let repository = makeRepository(
            handlerResult: .failure(NetworkError.http(code: .conflict))
        )

        let result = await repository.joinPool(joinPoolInput: makeJoinPoolInput())

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            #expect(error is JoinPoolError)
            if case JoinPoolError.alreadyJoined = error {
            } else {
                Issue.record("Expected JoinPoolError.alreadyJoined, got \(error)")
            }
        }
    }

    @Test("given a non-409 http error when joinPool is called then result fails with NetworkError.http preserved")
    func nonConflictHttpStaysAsNetworkError() async {
        let repository = makeRepository(
            handlerResult: .failure(NetworkError.http(code: .internalServerError))
        )

        let result = await repository.joinPool(joinPoolInput: makeJoinPoolInput())

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

    @Test("given a non-network error when joinPool is called then result fails with the original error")
    func nonNetworkErrorPassesThrough() async {
        let repository = makeRepository(
            handlerResult: .failure(StubError.boom)
        )

        let result = await repository.joinPool(joinPoolInput: makeJoinPoolInput())

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            #expect(error is StubError)
        }
    }

    private func makeRepository(handlerResult: Result<Void, Error>) -> PoolRemoteRepository {
        PoolRemoteRepository(
            poolRemoteDataSource: UnusedDataSource(),
            networkErrorHandler: StubNetworkErrorHandler(result: handlerResult)
        )
    }

    private func makeJoinPoolInput() -> JoinPoolInput {
        JoinPoolInput(
            poolId: "01K1PX1TX2NM1HG851S1V0QG6N",
            gamblerId: "01K1PX1TX2NM1HG851S1V0QG6P"
        )
    }
}

private enum StubError: Error {
    case boom
}

private final class StubNetworkErrorHandler: NetworkErrorHandler {
    private let result: Result<Void, Error>

    init(result: Result<Void, Error>) {
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

private final class UnusedDataSource: PoolRemoteDataSource {
    func getPool(id: String) async throws -> PoolResponse {
        fatalError("not used")
    }

    func createPool(creaePoolRequest: CreatePoolRequest) async throws -> CreatePoolResponse {
        fatalError("not used")
    }

    func joinPool(poolId: String, joinPoolRequest: JoinPoolRequest) async throws -> Void {
        fatalError("not used")
    }

    func deletePool(poolId: String, gamblerId: String) async throws -> Void {
        fatalError("not used")
    }
}
