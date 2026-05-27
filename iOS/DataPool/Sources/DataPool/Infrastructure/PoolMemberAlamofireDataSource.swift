import Alamofire
import Core
import Session

class PoolMemberAlamofireDataSource: PoolMemberRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession

    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }

    func getPoolMembers(
        poolId: String,
        next: String?
    ) async throws -> CursorPage<PoolMemberResponse> {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/members")!,
                parameters: ["next": next]
            )
            .validate()
            .responseDecodable(of: CursorPage<PoolMemberResponse>.self) { response in
                switch response.result {
                case .success(let page):
                    continuation.resume(returning: page)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func removeGambler(
        poolId: String,
        gamblerId: String
    ) async throws {
        return try await withUnsafeThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pools/\(poolId)/members/\(gamblerId)")!,
                method: .delete
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
