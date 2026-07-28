import Foundation

public class UploadAvatarUseCase {
    private let avatarRepository: AvatarRepository
    private let accountStorage: AccountStorage

    init(avatarRepository: AvatarRepository, accountStorage: AccountStorage) {
        self.avatarRepository = avatarRepository
        self.accountStorage = accountStorage
    }

    public func execute(imageData: Data) async -> Result<Void, Error> {
        do {
            guard let bundle = try await accountStorage.retrieve() else {
                return .failure(UploadAvatarError.noStoredAccount)
            }

            let uploadUrlResult = await avatarRepository.issueUploadUrl(
                accountId: bundle.accountId,
                contentLength: imageData.count
            )

            switch uploadUrlResult {
            case .success(let uploadUrl):
                return await avatarRepository.upload(imageData, to: uploadUrl)
            case .failure(let error):
                return .failure(error)
            }
        } catch let error {
            return .failure(error)
        }
    }
}

public enum UploadAvatarError: Error {
    case noStoredAccount
}
