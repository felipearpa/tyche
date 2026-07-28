import Alamofire
import Core
import Foundation

class AvatarAlamofireDataSource: AvatarRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    private let session: AuthenticatedSession

    init(urlBasePathProvider: URLBasePathProvider, session: AuthenticatedSession) {
        self.urlBasePathProvider = urlBasePathProvider
        self.session = session
    }

    func issueUploadUrl(accountId: String, request: AvatarUploadUrlRequest) async throws -> AvatarUploadUrlResponse {
        return try await withCheckedThrowingContinuation { continuation in
            session.request(
                urlBasePathProvider.prependBasePath("accounts/\(accountId)/avatar-upload-url")!,
                method: .post,
                parameters: request,
                encoder: JSONParameterEncoder.default
            )
            .validate()
            .responseDecodable(of: AvatarUploadUrlResponse.self) { response in
                switch response.result {
                case .success(let uploadUrlResponse):
                    continuation.resume(returning: uploadUrlResponse)
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
