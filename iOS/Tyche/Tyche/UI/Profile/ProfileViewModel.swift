import Account
import Combine
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

    private let currentAccountModel: CurrentAccountModel
    private let currentAccountCoordinator: CurrentAccountCoordinator
    private let onUploadAvatar: (Data) async -> Result<Void, Error>

    private var pendingUpload: (image: UIImage, data: Data)?
    private var cancellables = Set<AnyCancellable>()

    init(
        currentAccountModel: CurrentAccountModel,
        currentAccountCoordinator: CurrentAccountCoordinator,
        onUploadAvatar: @escaping (Data) async -> Result<Void, Error>
    ) {
        self.currentAccountModel = currentAccountModel
        self.currentAccountCoordinator = currentAccountCoordinator
        self.onUploadAvatar = onUploadAvatar

        currentAccountModel.$account
            .sink { [weak self] account in
                self?.accountId = account?.accountId ?? ""
                self?.username = account?.username ?? ""
                self?.email = account?.email ?? ""
            }
            .store(in: &cancellables)
    }

    /// The uploading photo shows optimistically; a failed upload falls back to what was
    /// there before, and an account without a photo falls back to the letter avatar.
    var avatarSource: ProfileAvatarSource {
        if uploadState.isLoading(), let pendingUpload {
            return .local(pendingUpload.image)
        }

        if !accountId.isEmpty, let url = AvatarURL.of(accountId: accountId) {
            return .remote(url)
        }

        return .letter
    }

    func refreshAccount() async {
        await currentAccountCoordinator.refresh(trigger: .profileOpened)
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
                // The upload flow has already seeded the shared avatar store, so every
                // visible surface shows the new photo before this state lands.
                pendingUpload = nil
                uploadState = .loaded(())
            case .failure(let error):
                uploadState = .failure(error)
            }
        }
    }
}
