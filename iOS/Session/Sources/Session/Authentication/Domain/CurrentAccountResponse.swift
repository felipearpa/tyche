struct CurrentAccountResponse: Decodable {
    let accountId: String
    let externalAccountId: String
    let email: String
    let username: String?
}
