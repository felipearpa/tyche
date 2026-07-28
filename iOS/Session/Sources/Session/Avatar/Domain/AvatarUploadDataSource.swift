import Foundation

protocol AvatarUploadDataSource {
    func upload(_ data: Data, to url: URL) async throws
}
