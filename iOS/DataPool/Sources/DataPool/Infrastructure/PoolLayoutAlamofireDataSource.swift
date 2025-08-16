import Foundation
import Alamofire
import Session
import Core

class PoolLayoutAlamofireDataSource : PoolLayoutRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession

    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }

    func getOpenPoolLayouts(next: String?, searchText: String?) async throws -> CursorPage<PoolLayoutResponse> {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("pool-layouts/open")!,
                parameters: ["next": next, "searchText": searchText],
            )
            .validate()
            .responseDecodable(
                of: CursorPage<PoolLayoutResponse>.self,
                decoder: JSONDecoder().withISODate(),
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
}
