extension CreatePoolInput {
    func toCreatePoolRequest() -> CreatePoolRequest {
        CreatePoolRequest(
            poolLayoutId: poolLayoutId,
            poolName: poolName.value,
            ownerGamblerId: ownerGamblerId,
        )
    }
}
