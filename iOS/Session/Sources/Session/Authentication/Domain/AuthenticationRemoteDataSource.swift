protocol AuthenticationRemoteDataSource {
    func linkAccount(request: LinkAccountRequest) async throws -> LinkAccountResponse
    func updateUsername(request: UpdateUsernameRequest) async throws
}
