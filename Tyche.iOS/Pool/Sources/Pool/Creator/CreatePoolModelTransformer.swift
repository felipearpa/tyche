import DataPool

extension CreatePoolModel {
    func toCreatePoolInput(ownerGamblerId: String) -> CreatePoolInput {
        CreatePoolInput(
            poolLayoutId: poolLayoutId,
            poolName: poolName,
            ownerGamblerId: ownerGamblerId
        )
    }
}
