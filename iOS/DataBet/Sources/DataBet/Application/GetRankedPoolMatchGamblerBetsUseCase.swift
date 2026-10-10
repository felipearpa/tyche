import Core

/// One page of a match's participant bets, ordered by each participant's total points in the
/// pool, greatest first, with gambler ID breaking ties. The server's order is final: callers do
/// not sort pages. A `nil` cursor requests the first page.
///
/// Failures specific to this mode arrive as `RankedMatchBetsError`; other failures keep the
/// repository's usual errors.
public class GetRankedPoolMatchGamblerBetsUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository

    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }

    public func execute(
        poolId: String,
        matchId: String,
        next: String? = nil
    ) async -> Result<CursorPage<PoolGamblerBet>, Error> {
        return await poolGamblerBetRepository.getRankedPoolMatchGamblerBets(
            poolId: poolId,
            matchId: matchId,
            next: next
        )
    }
}
