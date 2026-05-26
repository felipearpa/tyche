extension PoolResponse {
    func toPool() -> Pool {
        Pool(id: poolId, name: poolName, creatorGamblerId: creatorGamblerId, gamblerCount: gamblerCount)
    }
}
