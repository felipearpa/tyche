extension AccountResponse {
    func toAccountBundle() -> AccountBundle {
        AccountBundle(
            userId: self.userId,
            username: self.username
        )
    }
}
