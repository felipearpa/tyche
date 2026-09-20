import Foundation

protocol AvatarRepository {
    func issueUploadUrl(accountId: String, contentLength: Int) async -> Result<URL, Error>
    func upload(_ data: Data, to url: URL) async -> Result<Void, Error>
}
