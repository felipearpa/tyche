public protocol LoginRepository {
    func login(loginCredential: LoginCredential) async -> Result<LoginBundle, Error>
}
