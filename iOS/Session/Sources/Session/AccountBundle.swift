public struct AccountBundle: Codable {
    public let accountId: String
    public let externalAccountId: String
    public let email: String

    public init(accountId: String, externalAccountId: String, email: String) {
        self.accountId = accountId
        self.externalAccountId = externalAccountId
        self.email = email
    }

    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.accountId = try container.decode(String.self, forKey: .accountId)
        self.externalAccountId = try container.decode(String.self, forKey: .externalAccountId)
        self.email = try container.decodeIfPresent(String.self, forKey: .email) ?? ""
    }
}
