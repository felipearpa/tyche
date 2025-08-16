extension JoinPoolInput {
    func toJoinPoolRequest() -> JoinPoolRequest {
        JoinPoolRequest(gamblerId: gamblerId)
    }
}
