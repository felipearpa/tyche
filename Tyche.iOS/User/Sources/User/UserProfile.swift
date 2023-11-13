public struct UserProfile : Codable {
    public let userId: String
    public let username: String
    
    public init(userId: String, username: String) {
        self.userId = userId
        self.username = username
    }
}
