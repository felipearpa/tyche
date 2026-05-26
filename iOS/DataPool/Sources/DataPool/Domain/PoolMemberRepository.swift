import Core

public protocol PoolMemberRepository {
    func getPoolMembers(
        poolId: String,
        next: String?
    ) async -> Result<CursorPage<PoolMember>, Error>

    func removeGambler(
        poolId: String,
        gamblerId: String
    ) async -> Result<Void, Error>
}
