extension LoginCredential {
    func toRequest() -> LoginRequest {
        return LoginRequest(
            username: self.username.value,
            password: self.password.value
        )
    }
}
