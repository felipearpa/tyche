public class RemoveGamblerUseCase {
    private let poolMemberRepository: PoolMemberRepository

    public init(poolMemberRepository: PoolMemberRepository) {
        self.poolMemberRepository = poolMemberRepository
    }

    public func execute(poolId: String, gamblerId: String) async -> Result<Void, Error> {
        await poolMemberRepository.removeGambler(poolId: poolId, gamblerId: gamblerId)
    }
}
