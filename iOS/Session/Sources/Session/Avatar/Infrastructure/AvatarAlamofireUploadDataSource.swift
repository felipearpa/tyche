import Alamofire
import Foundation

/// Uploads to a presigned URL, whose query-string signature is the only authentication —
/// the request must not carry an `Authorization` header, so this bypasses `AuthenticatedSession`.
class AvatarAlamofireUploadDataSource: AvatarUploadDataSource {
    func upload(_ data: Data, to url: URL) async throws {
        return try await withCheckedThrowingContinuation { continuation in
            AF.upload(
                data,
                to: url,
                method: .put,
                headers: [
                    "Content-Type": AvatarUpload.contentType,
                    "Cache-Control": AvatarUpload.cacheControl,
                ]
            )
            .validate()
            .response { response in
                switch response.result {
                case .success:
                    continuation.resume(returning: ())
                case .failure(let error):
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
