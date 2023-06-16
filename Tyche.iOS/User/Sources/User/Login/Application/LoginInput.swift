struct LoginInput {
    let username: Username
    let password: Password
}

extension LoginInput {
    
    func toLoginCrendential() -> LoginCredential {
        return LoginCredential(username: self.username, password: self.password)
    }
}
