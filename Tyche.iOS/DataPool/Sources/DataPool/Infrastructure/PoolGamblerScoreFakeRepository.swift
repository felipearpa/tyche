import Core

public class PoolGamblerScoreFakeRepository: PoolGamblerScoreRepository {
    public init() {}
    
    public func getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
        return .success(
            CursorPage(
                items: dummyPoolGamblerScores(),
                next: nil
            )
        )
    }
    
    public func getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
        return .success(
            CursorPage(
                items: dummyPoolGamblerScores(),
                next: nil
            )
        )
    }
}
