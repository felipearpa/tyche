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

    func getPool(id: String) async throws -> PoolResponse {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(urlBasePathProvider.prependBasePath("pools/\(id)")!)
            .validate()
            .responseDecodable(
                of: PoolResponse.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let poolResponse):
                    continuation.resume(returning: poolResponse)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
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

    func joinPool(poolId: String, joinPoolRequest: JoinPoolRequest) async throws {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)")!,
                method: .post,
                parameters: joinPoolRequest,
                encoder: JSONParameterEncoder.default
            )
            .cURLDescription(calling: { curl in print(curl) })
            .validate()
            .response { response in
                switch response.result {
                case .success:
                    continuation.resume()
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
