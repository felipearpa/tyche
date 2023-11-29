import Alamofire
import Core
import Session

class PoolGamblerScoreAlamofireDateSource : PoolGamblerScoreRemoteDataSource {
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
        do {
            return try await withUnsafeThrowingContinuation { continuation in
                session.request(
                    urlBasePathProvider.prependBasePath("gambler/\(gamblerId)/pools")!,
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
        } catch {
            throw error
        }
    }
    
    func getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerScoreResponse> {
        do {
            return try await withUnsafeThrowingContinuation { continuation in
                session.request(
                    urlBasePathProvider.prependBasePath("pool/\(poolId)/gamblers")!,
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
        } catch {
            throw error
        }
    }
}
