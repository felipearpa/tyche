protocol AuthenticationRemoteDataSource {
    func linkAccount(request: LinkAccountRequest) async throws -> LinkAccountResponse
}
