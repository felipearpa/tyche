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
    
    func getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async throws -> CursorPage<PoolGamblerBetResponse> {
        do {
            return try await withUnsafeThrowingContinuation { continuation in
                session.request(
                    urlBasePathProvider.prependBasePath("pool/\(poolId)/gambler/\(gamblerId)/bets")!,
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
        } catch {
            throw error
        }
    }
    
    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse {
        do {
            return try await withUnsafeThrowingContinuation { continuation in
                session.request(
                    urlBasePathProvider.prependBasePath("bet")!,
                    method: .patch,
                    parameters: betRequest,
                    encoder: JSONParameterEncoder.default
                )
                .cURLDescription(calling: { curl in print(curl) })
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
        } catch {
            throw error
        }
    }
}
