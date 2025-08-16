import Alamofire
import Core
import Session

class PoolGamblerScoreAlamofireDateSource: PoolGamblerScoreRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession
    
    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }
    
    func getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerScoreResponse> {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("gamblers/\(gamblerId)/pools")!,
                parameters: ["next": next, "searchText": searchText]
            )
            .validate()
            .responseDecodable(of: CursorPage<PoolGamblerScoreResponse>.self) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerScoreResponse> {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers")!,
                parameters: ["next": next, "searchText": searchText]
            )
            .validate()
            .responseDecodable(of: CursorPage<PoolGamblerScoreResponse>.self) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    func getPoolGamblerScore(
        poolId: String,
        gamblerId: String
    ) async throws -> PoolGamblerScoreResponse {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers/\(gamblerId)")!
            )
            .validate()
            .responseDecodable(of: PoolGamblerScoreResponse.self) { response in
                switch response.result {
                case .success(let poolGamblerScore):
                    continuation.resume(returning: poolGamblerScore)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
