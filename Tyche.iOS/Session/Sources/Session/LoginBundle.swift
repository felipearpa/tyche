public struct LoginBundle {
    public let authBundle: AuthBundle
    public let accountBundle: AccountBundle
    
    public init(authBundle: AuthBundle, accountBundle: AccountBundle) {
        self.authBundle = authBundle
        self.accountBundle = accountBundle
    }
}
