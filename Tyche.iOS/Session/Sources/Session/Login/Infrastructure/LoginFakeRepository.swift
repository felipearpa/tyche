public class LoginFakeRepository: LoginRepository {
    public init() {}
    
    public func login(loginCredential: LoginCredential) async -> Result<LoginBundle, Error> {
        return .success(
            LoginBundle(
                authBundle: AuthBundle(token: ""),
                accountBundle: AccountBundle(userId: "", username: "")
            )
        )
    }
}
