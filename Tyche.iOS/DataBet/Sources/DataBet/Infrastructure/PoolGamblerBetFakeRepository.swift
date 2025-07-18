import Core

public class PoolGamblerBetFakeRepository: PoolGamblerBetRepository {
    public init() {}
    
    public func getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return .success(
            CursorPage(
                items: dummyPoolGamblerBets(),
                next: nil
            )
        )
    }

    public func getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return .success(
            CursorPage(
                items: dummyPoolGamblerBets(),
                next: nil
            )
        )
    }

    public func bet(bet: Bet) async -> Result<PoolGamblerBet, Error> {
        return .success(dummyPoolGamblerBet())
    }
}
