struct LoginResponse : Decodable {
    let token: String
    let user: UserResponse
}

extension LoginResponse {
    
    func toProfile() -> LoginProfile {
        return LoginProfile(token: self.token, user: self.user.toProfile())
    }
}
