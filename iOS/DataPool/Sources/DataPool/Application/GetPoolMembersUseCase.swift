import Core

public class GetPoolMembersUseCase {
    private let poolMemberRepository: PoolMemberRepository

    public init(poolMemberRepository: PoolMemberRepository) {
        self.poolMemberRepository = poolMemberRepository
    }

    public func execute(
        poolId: String,
        next: String? = nil
    ) async -> Result<CursorPage<PoolMember>, Error> {
        return await poolMemberRepository.getPoolMembers(poolId: poolId, next: next)
    }
}
