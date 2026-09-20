import Core
import Foundation

class AvatarRemoteRepository: AvatarRepository {
    private let avatarRemoteDataSource: AvatarRemoteDataSource
    private let avatarUploadDataSource: AvatarUploadDataSource
    private let networkErrorHandler: NetworkErrorHandler

    init(
        avatarRemoteDataSource: AvatarRemoteDataSource,
        avatarUploadDataSource: AvatarUploadDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.avatarRemoteDataSource = avatarRemoteDataSource
        self.avatarUploadDataSource = avatarUploadDataSource
        self.networkErrorHandler = networkErrorHandler
    }

    func issueUploadUrl(accountId: String, contentLength: Int) async -> Result<URL, Error> {
        return await networkErrorHandler.handle {
            let response = try await avatarRemoteDataSource.issueUploadUrl(
                accountId: accountId,
                request: AvatarUploadUrlRequest(contentLength: contentLength)
            )

            guard let url = URL(string: response.url) else {
                throw NetworkError.remoteCommunication
            }

            return url
        }
    }

    func upload(_ data: Data, to url: URL) async -> Result<Void, Error> {
        return await networkErrorHandler.handle {
            try await avatarUploadDataSource.upload(data, to: url)
        }
    }
}
