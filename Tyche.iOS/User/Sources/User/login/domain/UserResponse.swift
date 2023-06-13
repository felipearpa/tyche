struct UserResponse : Decodable {
    let userId: String
    let username: String
}

extension UserResponse {
    
    func toProfile() -> UserProfile {
        return UserProfile(userId: self.userId, username: self.username)
    }
}
