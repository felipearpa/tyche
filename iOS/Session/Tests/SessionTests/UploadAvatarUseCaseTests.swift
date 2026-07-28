import Foundation
import Testing
@testable import Session

@Suite("UploadAvatarUseCase")
struct UploadAvatarUseCaseTests {

    @Test("given a stored account when executed then a url is issued for it and the image is uploaded")
    func uploadsThroughTheIssuedUrl() async {
        let repository = SpyAvatarRepository()
        let useCase = UploadAvatarUseCase(
            avatarRepository: repository,
            accountStorage: FakeAccountStorage(bundle: storedBundle)
        )
        let imageData = Data([1, 2, 3])

        let result = await useCase.execute(imageData: imageData)

        #expect((try? result.get()) != nil)
        #expect(repository.issuedAccountIds == ["account-1"])
        #expect(repository.issuedContentLengths == [3])
        #expect(repository.uploadedData == [imageData])
        #expect(repository.uploadedUrls == [SpyAvatarRepository.issuedUrl])
    }

    @Test("given no stored account when executed then it fails and nothing is issued nor uploaded")
    func failsWithoutStoredAccount() async {
        let repository = SpyAvatarRepository()
        let useCase = UploadAvatarUseCase(
            avatarRepository: repository,
            accountStorage: FakeAccountStorage(bundle: nil)
        )

        let result = await useCase.execute(imageData: Data([1]))

        guard case .failure(let error) = result else {
            Issue.record("expected a failure")
            return
        }
        #expect(error as? UploadAvatarError == .noStoredAccount)
        #expect(repository.issuedAccountIds.isEmpty)
        #expect(repository.uploadedData.isEmpty)
    }

    @Test("given the presign call fails when executed then the error propagates and nothing is uploaded")
    func propagatesPresignFailure() async {
        let repository = SpyAvatarRepository()
        repository.issueResult = .failure(TestError())
        let useCase = UploadAvatarUseCase(
            avatarRepository: repository,
            accountStorage: FakeAccountStorage(bundle: storedBundle)
        )

        let result = await useCase.execute(imageData: Data([1]))

        guard case .failure(let error) = result else {
            Issue.record("expected a failure")
            return
        }
        #expect(error is TestError)
        #expect(repository.uploadedData.isEmpty)
    }

    @Test("given the upload PUT fails when executed then the error propagates")
    func propagatesUploadFailure() async {
        let repository = SpyAvatarRepository()
        repository.uploadResult = .failure(TestError())
        let useCase = UploadAvatarUseCase(
            avatarRepository: repository,
            accountStorage: FakeAccountStorage(bundle: storedBundle)
        )

        let result = await useCase.execute(imageData: Data([1]))

        guard case .failure(let error) = result else {
            Issue.record("expected a failure")
            return
        }
        #expect(error is TestError)
    }
}

private let storedBundle = AccountBundle(
    accountId: "account-1",
    externalAccountId: "external-1",
    email: "gambler@tyche.com"
)

private struct TestError: Error {}

private final class SpyAvatarRepository: AvatarRepository {
    static let issuedUrl = URL(string: "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg")!

    var issueResult: Result<URL, Error> = .success(SpyAvatarRepository.issuedUrl)
    var uploadResult: Result<Void, Error> = .success(())

    private(set) var issuedAccountIds: [String] = []
    private(set) var issuedContentLengths: [Int] = []
    private(set) var uploadedData: [Data] = []
    private(set) var uploadedUrls: [URL] = []

    func issueUploadUrl(accountId: String, contentLength: Int) async -> Result<URL, Error> {
        issuedAccountIds.append(accountId)
        issuedContentLengths.append(contentLength)
        return issueResult
    }

    func upload(_ data: Data, to url: URL) async -> Result<Void, Error> {
        uploadedData.append(data)
        uploadedUrls.append(url)
        return uploadResult
    }
}

private struct FakeAccountStorage: AccountStorage {
    let bundle: AccountBundle?

    func store(accountBundle: AccountBundle) async throws {}
    func delete() async throws {}
    func retrieve() async throws -> AccountBundle? { bundle }
}
