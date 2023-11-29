public struct LoginCredential {
    public let username: Username
    public let password: Password
    
    public init(username: Username, password: Password) {
        self.username = username
        self.password = password
    }
}
