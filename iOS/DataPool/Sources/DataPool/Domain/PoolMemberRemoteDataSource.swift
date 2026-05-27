import Core

protocol PoolMemberRemoteDataSource {
    func getPoolMembers(
        poolId: String,
        next: String?
    ) async throws -> CursorPage<PoolMemberResponse>

    func removeGambler(
        poolId: String,
        gamblerId: String
    ) async throws
}
