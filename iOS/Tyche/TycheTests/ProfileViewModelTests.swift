import Account
import Foundation
import Session
import Testing
import UIKit
@testable import Tyche

@MainActor
@Suite("ProfileViewModel", .serialized)
struct ProfileViewModelTests {

    @Test("given no stored account when observed then the letter avatar is shown")
    func letterFallbackWithoutStoredAccount() async {
        let viewModel = ProfileViewModel(
            currentAccountModel: .preview(account: nil),
            currentAccountCoordinator: .preview(account: nil),
            onUploadAvatar: { _ in .success(()) }
        )

        #expect(viewModel.avatarSource == .letter)
    }

    @Test("given a stored account when observed then the avatar url derives from the account id")
    func remoteAvatarUrlDerivesFromAccountId() async {
        let viewModel = ProfileViewModel(
            currentAccountModel: .preview(account: storedBundle),
            currentAccountCoordinator: .preview(account: storedBundle),
            onUploadAvatar: { _ in .success(()) }
        )

        await waitUntil { viewModel.accountId == "account-1" }

        #expect(
            viewModel.avatarSource
                == .remote(URL(string: "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg")!)
        )
        #expect(viewModel.username == "ElGoleador")
        #expect(viewModel.email == "gambler@tyche.com")
    }

    @Test("given an upload begins then the new photo shows optimistically and success returns to the remote source")
    func successfulUploadShowsOptimisticallyThenRemote() async {
        let uploader = SpyUploader(results: [.success(())])
        let viewModel = ProfileViewModel(
            currentAccountModel: .preview(account: storedBundle),
            currentAccountCoordinator: .preview(account: storedBundle),
            onUploadAvatar: uploader.upload
        )
        await waitUntil { viewModel.accountId == "account-1" }
        let newPhoto = avatarImage(color: .systemGreen)
        let payload = newPhoto.jpegData(compressionQuality: 0.8)!

        let task = viewModel.beginUpload(image: newPhoto, data: payload)

        #expect(viewModel.uploadState.isLoading())
        #expect(viewModel.avatarSource == .local(newPhoto))

        await task.value

        #expect(viewModel.uploadState.isLoaded())
        #expect(
            viewModel.avatarSource
                == .remote(URL(string: "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg")!)
        )
        #expect(uploader.receivedData == [payload])
    }

    @Test("given the upload fails then the previous avatar is preserved and retry can succeed")
    func failedUploadPreservesPreviousAvatarAndRetries() async {
        let uploader = SpyUploader(results: [.failure(TestError()), .success(())])
        let viewModel = ProfileViewModel(
            currentAccountModel: .preview(account: storedBundle),
            currentAccountCoordinator: .preview(account: storedBundle),
            onUploadAvatar: uploader.upload
        )
        await waitUntil { viewModel.accountId == "account-1" }
        let previousSource = viewModel.avatarSource
        let newPhoto = avatarImage(color: .systemBlue)

        await viewModel.beginUpload(image: newPhoto, data: Data([1])).value

        #expect(viewModel.uploadState.isFailure())
        #expect(viewModel.avatarSource == previousSource)

        await viewModel.retryUpload().value

        #expect(viewModel.uploadState.isLoaded())
        #expect(uploader.receivedData.count == 2)
    }

    @Test("given the upload failed when the error is dismissed then the pending photo is discarded")
    func dismissingErrorDiscardsPendingPhoto() async {
        let uploader = SpyUploader(results: [.failure(TestError())])
        let viewModel = ProfileViewModel(
            currentAccountModel: .preview(account: storedBundle),
            currentAccountCoordinator: .preview(account: storedBundle),
            onUploadAvatar: uploader.upload
        )
        await waitUntil { viewModel.accountId == "account-1" }
        let previousSource = viewModel.avatarSource

        await viewModel.beginUpload(image: avatarImage(color: .systemOrange), data: Data([1])).value
        viewModel.dismissUploadError()

        #expect(viewModel.uploadState.isIdle())
        #expect(viewModel.avatarSource == previousSource)
    }
}

private let storedBundle = AccountBundle(
    accountId: "account-1",
    externalAccountId: "external-1",
    email: "gambler@tyche.com",
    username: "ElGoleador"
)

private struct TestError: Error {}

private func avatarImage(color: UIColor) -> UIImage {
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    let renderer = UIGraphicsImageRenderer(size: CGSize(width: 512, height: 512), format: format)
    return renderer.image { context in
        color.setFill()
        context.fill(CGRect(x: 0, y: 0, width: 512, height: 512))
    }
}

private func waitUntil(
    timeoutTicks: Int = 5000,
    _ condition: @escaping () -> Bool
) async {
    var ticks = 0
    while !condition() && ticks < timeoutTicks {
        await Task.yield()
        ticks += 1
    }
}

private final class SpyUploader {
    private var results: [Result<Void, Error>]
    private(set) var receivedData: [Data] = []

    init(results: [Result<Void, Error>]) {
        self.results = results
    }

    func upload(_ data: Data) async -> Result<Void, Error> {
        receivedData.append(data)
        return results.isEmpty ? .success(()) : results.removeFirst()
    }
}
