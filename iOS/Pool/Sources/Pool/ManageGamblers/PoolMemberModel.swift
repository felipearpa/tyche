import Foundation
import DataPool

public struct PoolMemberModel: Identifiable, Hashable, Codable, Sendable {
    public let gamblerId: String
    public let gamblerUsername: String
    public let gamblerEmail: String
    public let isOwner: Bool

    public var id: String { gamblerId }

    public init(
        gamblerId: String,
        gamblerUsername: String,
        gamblerEmail: String,
        isOwner: Bool = false
    ) {
        self.gamblerId = gamblerId
        self.gamblerUsername = gamblerUsername
        self.gamblerEmail = gamblerEmail
        self.isOwner = isOwner
    }
}

extension PoolMember {
    func toPoolMemberModel() -> PoolMemberModel {
        PoolMemberModel(
            gamblerId: gamblerId,
            gamblerUsername: gamblerUsername,
            gamblerEmail: gamblerEmail,
            isOwner: isOwner
        )
    }
}

func poolMemberPlaceholderModel() -> PoolMemberModel {
    PoolMemberModel(
        gamblerId: UUID().uuidString,
        gamblerUsername: "placeholder",
        gamblerEmail: "placeholder@example.com"
    )
}
