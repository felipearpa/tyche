import Foundation
import Alamofire
import Core

class AuthenticationAlamofireDataSource: AuthenticationRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession
    
    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }
    
    func linkAccount(request: LinkAccountRequest) async throws -> LinkAccountResponse {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("account/link")!,
                method: .post,
                parameters: request,
                encoder: JSONParameterEncoder.default
            )
            .cURLDescription(calling: { curl in print(curl) })
            .validate()
            .responseDecodable(
                of: LinkAccountResponse.self,
                decoder: JSONDecoder().withISODate()
            ) { response in
                switch response.result {
                case .success(let response):
                    continuation.resume(returning: response)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
