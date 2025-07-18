extension JoinPoolInput {
    func toJoinPoolRequest() -> JoinPoolRequest {
        JoinPoolRequest(poolId: poolId, gamblerId: gamblerId)
    }
}
