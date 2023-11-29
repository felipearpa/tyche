extension LoginResponse {
    func toLoginBundle() -> LoginBundle {
        LoginBundle(
            authBundle: AuthBundle(token: self.token),
            accountBundle: self.user.toAccountBundle()
        )
    }
}
