public struct AccountBundle: Codable {
    public let accountId: String
    public let externalAccountId: String
    public let email: String
    private let storedUsername: String

    public var username: String {
        storedUsername.isEmpty ? email : storedUsername
    }

    public init(accountId: String, externalAccountId: String, email: String, username: String = "") {
        self.accountId = accountId
        self.externalAccountId = externalAccountId
        self.email = email
        self.storedUsername = username
    }

    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.accountId = try container.decode(String.self, forKey: .accountId)
        self.externalAccountId = try container.decode(String.self, forKey: .externalAccountId)
        self.email = try container.decodeIfPresent(String.self, forKey: .email) ?? ""
        self.storedUsername = try container.decodeIfPresent(String.self, forKey: .username) ?? ""
    }

    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(accountId, forKey: .accountId)
        try container.encode(externalAccountId, forKey: .externalAccountId)
        try container.encode(email, forKey: .email)
        try container.encode(storedUsername, forKey: .username)
    }

    public func withUsername(_ value: String) -> AccountBundle {
        AccountBundle(
            accountId: accountId,
            externalAccountId: externalAccountId,
            email: email,
            username: value
        )
    }

    private enum CodingKeys: String, CodingKey {
        case accountId
        case externalAccountId
        case email
        case username
    }
}
