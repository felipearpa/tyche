extension CreatePoolResponse {
    func toCreatePoolOutput() -> CreatePoolOutput {
        CreatePoolOutput(
            poolId: poolId,
            poolName: poolName,
        )
    }
}
