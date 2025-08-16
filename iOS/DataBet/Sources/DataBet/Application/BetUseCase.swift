import Core

public class BetUseCase {
    private let poolGamblerBetRepository: PoolGamblerBetRepository
    
    public init(poolGamblerBetRepository: PoolGamblerBetRepository) {
        self.poolGamblerBetRepository = poolGamblerBetRepository
    }
    
    public func execute(bet: Bet) async -> Result<PoolGamblerBet, Error> {
        return await poolGamblerBetRepository.bet(bet: bet)
    }
}
