public struct LoginCredential {
    let username: Username
    let password: Password
}

extension LoginCredential {
    
    func toRequest() -> LoginRequest {
        return LoginRequest(
            username: self.username.value,
            password: self.password.value
        )
    }
}
