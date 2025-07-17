public class CreatePoolUseCase {
    private let poolRepository: PoolRepository

    public init(poolRepository: PoolRepository) {
        self.poolRepository = poolRepository
    }

    public func execute(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, Error> {
        await poolRepository.createPool(createPoolInput: createPoolInput)
    }
}
