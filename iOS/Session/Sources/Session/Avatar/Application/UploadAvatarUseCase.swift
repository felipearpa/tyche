import Foundation

/// Installs a successfully uploaded avatar into the shared image cache so every visible
/// surface updates from memory before the upload reports success.
public typealias InstallUploadedAvatar = @Sendable (_ imageData: Data, _ accountId: String) async -> Void

public class UploadAvatarUseCase {
    private let avatarRepository: AvatarRepository
    private let currentAccountCoordinator: CurrentAccountCoordinator
    private let installUploadedAvatar: InstallUploadedAvatar

    init(
        avatarRepository: AvatarRepository,
        currentAccountCoordinator: CurrentAccountCoordinator,
        installUploadedAvatar: @escaping InstallUploadedAvatar
    ) {
        self.avatarRepository = avatarRepository
        self.currentAccountCoordinator = currentAccountCoordinator
        self.installUploadedAvatar = installUploadedAvatar
    }

    public func execute(imageData: Data) async -> Result<Void, Error> {
        guard let account = await currentAccountCoordinator.account else {
            return .failure(UploadAvatarError.noStoredAccount)
        }

        // Captured before the PUT so a logout completing mid-upload cannot redirect the seed.
        let accountId = account.accountId

        let uploadUrlResult = await avatarRepository.issueUploadUrl(
            accountId: accountId,
            contentLength: imageData.count
        )

        switch uploadUrlResult {
        case .success(let uploadUrl):
            let uploadResult = await avatarRepository.upload(imageData, to: uploadUrl)
            if case .success = uploadResult {
                await installUploadedAvatar(imageData, accountId)
            }
            return uploadResult
        case .failure(let error):
            return .failure(error)
        }
    }
}

public enum UploadAvatarError: Error {
    case noStoredAccount
}
