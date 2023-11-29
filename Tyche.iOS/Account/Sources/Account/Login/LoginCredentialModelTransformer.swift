import Session

extension LoginCredentialModel {
    func toLoginCredential() -> LoginCredential {
        LoginCredential(
            username: Username(self.username)!,
            password: Password(self.password)!
        )
    }
}
