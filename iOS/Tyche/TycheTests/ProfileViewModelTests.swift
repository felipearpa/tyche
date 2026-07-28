import Account
import Foundation
import Session
import Testing
import UIKit
@testable import Tyche

@MainActor
@Suite("ProfileViewModel")
struct ProfileViewModelTests {

    @Test("given no stored account when loaded then the letter avatar is shown")
    func letterFallbackWithoutStoredAccount() async {
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: nil),
            onUploadAvatar: { _ in .success(()) }
        )

        await viewModel.loadAccount()

        #expect(viewModel.avatarSource == .letter)
    }

    @Test("given a stored account when loaded then the avatar url derives from the account id")
    func remoteAvatarUrlDerivesFromAccountId() async {
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: storedBundle),
            onUploadAvatar: { _ in .success(()) }
        )

        await viewModel.loadAccount()

        #expect(
            viewModel.avatarSource
                == .remote(URL(string: "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg")!)
        )
        #expect(viewModel.username == "ElGoleador")
        #expect(viewModel.email == "gambler@tyche.com")
    }

    @Test("given an upload begins then the new photo shows immediately and stays after success without a re-fetch")
    func successfulUploadRendersLocallyWithoutRefetch() async {
        let uploader = SpyUploader(results: [.success(())])
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: storedBundle),
            onUploadAvatar: uploader.upload
        )
        await viewModel.loadAccount()
        let newPhoto = UIImage()
        let payload = Data([9, 9])

        let task = viewModel.beginUpload(image: newPhoto, data: payload)

        #expect(viewModel.uploadState.isLoading())
        #expect(viewModel.avatarSource == .local(newPhoto))

        await task.value

        #expect(viewModel.uploadState.isLoaded())
        #expect(viewModel.avatarSource == .local(newPhoto))
        #expect(uploader.receivedData == [payload])
    }

    @Test("given the upload fails then the previous avatar is preserved and retry can succeed")
    func failedUploadPreservesPreviousAvatarAndRetries() async {
        let uploader = SpyUploader(results: [.failure(TestError()), .success(())])
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: storedBundle),
            onUploadAvatar: uploader.upload
        )
        await viewModel.loadAccount()
        let previousSource = viewModel.avatarSource
        let newPhoto = UIImage()

        await viewModel.beginUpload(image: newPhoto, data: Data([1])).value

        #expect(viewModel.uploadState.isFailure())
        #expect(viewModel.avatarSource == previousSource)

        await viewModel.retryUpload().value

        #expect(viewModel.uploadState.isLoaded())
        #expect(viewModel.avatarSource == .local(newPhoto))
        #expect(uploader.receivedData.count == 2)
    }

    @Test("given an upload succeeds then the avatar version bumps so navigation chrome refetches")
    func successfulUploadBumpsAvatarVersion() async {
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: storedBundle),
            onUploadAvatar: { _ in .success(()) }
        )
        await viewModel.loadAccount()
        let versionBefore = AvatarVersion.shared.value

        await viewModel.beginUpload(image: UIImage(), data: Data([1])).value

        #expect(AvatarVersion.shared.value == versionBefore + 1)
    }

    @Test("given an upload fails then the avatar version does not bump")
    func failedUploadDoesNotBumpAvatarVersion() async {
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: storedBundle),
            onUploadAvatar: { _ in .failure(TestError()) }
        )
        await viewModel.loadAccount()
        let versionBefore = AvatarVersion.shared.value

        await viewModel.beginUpload(image: UIImage(), data: Data([1])).value

        #expect(AvatarVersion.shared.value == versionBefore)
    }

    @Test("given the upload failed when the error is dismissed then the pending photo is discarded")
    func dismissingErrorDiscardsPendingPhoto() async {
        let uploader = SpyUploader(results: [.failure(TestError())])
        let viewModel = ProfileViewModel(
            accountStorage: FakeAccountStorage(bundle: storedBundle),
            onUploadAvatar: uploader.upload
        )
        await viewModel.loadAccount()
        let previousSource = viewModel.avatarSource

        await viewModel.beginUpload(image: UIImage(), data: Data([1])).value
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

private struct FakeAccountStorage: AccountStorage {
    let bundle: AccountBundle?

    func store(accountBundle: AccountBundle) async throws {}
    func delete() async throws {}
    func retrieve() async throws -> AccountBundle? { bundle }
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
