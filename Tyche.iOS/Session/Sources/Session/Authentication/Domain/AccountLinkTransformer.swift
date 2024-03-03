extension AccountLink {
    func toLinkAccountRequest() -> LinkAccountRequest {
        LinkAccountRequest(
            email: self.email.value,
            externalAccountId: self.externalAccountId
        )
    }
}
