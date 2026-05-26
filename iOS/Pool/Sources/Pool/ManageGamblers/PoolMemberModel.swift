import Foundation
import DataPool

public struct PoolMemberModel: Identifiable, Hashable, Codable, Sendable {
    public let gamblerId: String
    public let gamblerUsername: String
    public let gamblerEmail: String

    public var id: String { gamblerId }
}

extension PoolMember {
    func toPoolMemberModel() -> PoolMemberModel {
        PoolMemberModel(
            gamblerId: gamblerId,
            gamblerUsername: gamblerUsername,
            gamblerEmail: gamblerEmail
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
