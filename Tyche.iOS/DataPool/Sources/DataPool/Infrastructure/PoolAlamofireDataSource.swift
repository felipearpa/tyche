import Foundation
import Alamofire
import Session
import Core

class PoolAlamofireDataSource : PoolRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession

    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }

    func createPool(creaePoolRequest: CreatePoolRequest) async throws -> CreatePoolResponse {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools")!,
                method: .post,
                parameters: creaePoolRequest,
                encoder: JSONParameterEncoder.default
            )
            .cURLDescription(calling: { curl in print(curl) })
            .validate()
            .responseDecodable(
                of: CreatePoolResponse.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let createPoolResponse):
                    continuation.resume(returning: createPoolResponse)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
