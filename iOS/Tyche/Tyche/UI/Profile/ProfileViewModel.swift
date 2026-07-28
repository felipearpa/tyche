import Account
import Foundation
import Session
import UIKit
import ViewingState

enum ProfileAvatarSource: Equatable {
    case local(UIImage)
    case remote(URL)
    case letter
}

@MainActor
class ProfileViewModel: ObservableObject {
    @Published private(set) var accountId: String = ""
    @Published private(set) var username: String = ""
    @Published private(set) var email: String = ""
    @Published private(set) var uploadState: LoadState<Void> = .idle

    private let accountStorage: AccountStorage
    private let onUploadAvatar: (Data) async -> Result<Void, Error>

    private var localAvatarImage: UIImage?
    private var pendingUpload: (image: UIImage, data: Data)?

    init(
        accountStorage: AccountStorage,
        onUploadAvatar: @escaping (Data) async -> Result<Void, Error>
    ) {
        self.accountStorage = accountStorage
        self.onUploadAvatar = onUploadAvatar
    }

    /// The uploading photo shows optimistically; a failed upload falls back to what was
    /// there before, and an account without a photo falls back to the letter avatar.
    var avatarSource: ProfileAvatarSource {
        if uploadState.isLoading(), let pendingUpload {
            return .local(pendingUpload.image)
        }

        if let localAvatarImage {
            return .local(localAvatarImage)
        }

        if !accountId.isEmpty, let url = AvatarURL.of(accountId: accountId) {
            return .remote(url)
        }

        return .letter
    }

    func loadAccount() async {
        let bundle = try? await accountStorage.retrieve()
        accountId = bundle?.accountId ?? ""
        username = bundle?.username ?? ""
        email = bundle?.email ?? ""
    }

    func applyUsername(_ newUsername: String) {
        username = newUsername
    }

    @discardableResult
    func beginUpload(image: UIImage, data: Data) -> Task<Void, Never> {
        pendingUpload = (image, data)
        return performUpload()
    }

    @discardableResult
    func retryUpload() -> Task<Void, Never> {
        performUpload()
    }

    func dismissUploadError() {
        pendingUpload = nil
        uploadState = .idle
    }

    private func performUpload() -> Task<Void, Never> {
        uploadState = .loading

        return Task {
            guard let pending = pendingUpload else {
                uploadState = .idle
                return
            }

            let result = await onUploadAvatar(pending.data)

            switch result {
            case .success:
                localAvatarImage = pending.image
                pendingUpload = nil
                uploadState = .loaded(())
                AvatarVersion.shared.bump()
            case .failure(let error):
                uploadState = .failure(error)
            }
        }
    }
}
