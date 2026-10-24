import Foundation

public struct AvatarURL: Sendable {
    private let base: String

    public init(base: String) {
        self.base = base
    }

    public func of(accountId: String) -> URL? {
        guard !accountId.isEmpty else { return nil }
        return URL(string: "\(base)/avatars/\(accountId).jpg")
    }
}
