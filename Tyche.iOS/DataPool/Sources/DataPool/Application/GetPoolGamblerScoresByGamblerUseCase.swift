import Core

public class GetPoolGamblerScoresByGamblerUseCase {
    private let poolGamblerScoreRepository: PoolGamblerScoreRepository
    
    public init(poolGamblerScoreRepository: PoolGamblerScoreRepository) {
        self.poolGamblerScoreRepository = poolGamblerScoreRepository
    }
    
    public func execute(
        gamblerId: String,
        next: String? = nil,
        searchText: String? = nil
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
        return await poolGamblerScoreRepository.getPoolGamblerScoresByGambler(
            gamblerId: gamblerId,
            next: next,
            searchText: searchText
        )
    }
}
