public struct AccountBundle: Codable {
    public let accountId: String
    public let externalAccountId: String
    
    public init(accountId: String, externalAccountId: String) {
        self.accountId = accountId
        self.externalAccountId = externalAccountId
    }
}
