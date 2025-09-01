import Foundation
import Alamofire
import Core
import Session

class PoolGamblerBetAlamofireDataSource: PoolGamblerBetRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession
    
    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }
    
    func getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse> {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers/\(gamblerId)/bets/pending")!,
                parameters: ["next": next, "searchText": searchText]
            )
            .validate()
            .responseDecodable(
                of: CursorPage<PoolGamblerBetResponse>.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func getFinishedPoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> Core.CursorPage<PoolGamblerBetResponse> {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/gamblers/\(gamblerId)/bets/finished")!,
                parameters: ["next": next, "searchText": searchText]
            )
            .validate()
            .responseDecodable(
                of: CursorPage<PoolGamblerBetResponse>.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("bets")!,
                method: .patch,
                parameters: betRequest,
                encoder: JSONParameterEncoder.default
            )
            .validate()
            .responseDecodable(
                of: PoolGamblerBetResponse.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let poolGamblerBetResponse):
                    continuation.resume(returning: poolGamblerBetResponse)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
