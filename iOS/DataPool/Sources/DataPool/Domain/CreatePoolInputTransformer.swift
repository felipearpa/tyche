extension CreatePoolInput {
    func toCreatePoolRequest() -> CreatePoolRequest {
        CreatePoolRequest(
            poolLayoutId: poolLayoutId,
            poolName: poolName,
            ownerGamblerId: ownerGamblerId,
        )
    }
}
