import Foundation

private final class PreviewAvatarRepository: AvatarRepository {
    func issueUploadUrl(accountId: String, contentLength: Int) async -> Result<URL, Error> {
        .success(URL(string: "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/preview.jpg")!)
    }

    func upload(_ data: Data, to url: URL) async -> Result<Void, Error> { .success(()) }
}

public extension UploadAvatarUseCase {
    static func preview() -> UploadAvatarUseCase {
        UploadAvatarUseCase(
            avatarRepository: PreviewAvatarRepository(),
            accountStorage: PreviewAccountStorage()
        )
    }
}
