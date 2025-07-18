public class JoinPoolUseCase {
    private let poolRepository: PoolRepository

    public init(poolRepository: PoolRepository) {
        self.poolRepository = poolRepository
    }

    public func execute(joinPoolInput: JoinPoolInput) async -> Result<Void, Error> {
        await poolRepository.joinPool(joinPoolInput: joinPoolInput)
    }
}
