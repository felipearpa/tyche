import Foundation
import UI
import Core
import DataBet

class PoolGamblerBetItemViewModel: ObservableObject {
    private let betUseCase: BetUseCase
    
    @Published @MainActor private(set) var state: EditableViewState<PoolGamblerBetModel>
    
    @MainActor
    public init(poolGamblerBet: PoolGamblerBetModel, betUseCase: BetUseCase) {
        self.betUseCase = betUseCase
        self.state = .initial(poolGamblerBet)
    }
    
    @MainActor
    func retryBet() {
        guard case .failure(_, failed: let poolGamblerBet, _) = state else { return }
        guard let betScore = poolGamblerBet.betScore else { return }
        bet(betScore: betScore)
    }
    
    @MainActor
    func bet(betScore: TeamScore<Int>) {
        Task { [self] in
            var currentPoolGamblerBet = state.relevantValue()
            
            let targetPoolGamblerBet = currentPoolGamblerBet.copy { builder in
                builder.betScore = betScore
            }
            
            state = .saving(current: currentPoolGamblerBet, target: targetPoolGamblerBet)
            
            let bet = Bet(
                poolId: targetPoolGamblerBet.poolId,
                gamblerId: targetPoolGamblerBet.gamblerId,
                matchId: targetPoolGamblerBet.matchId,
                homeTeamBet: BetScore(betScore.homeTeamValue)!,
                awayTeamBet: BetScore(betScore.awayTeamValue)!
            )
            let result = await betUseCase.execute(bet: bet)
            
            switch result {
            case .success(let updatedPoolGamblerBet):
                state = .success(old: currentPoolGamblerBet, succeeded: updatedPoolGamblerBet.toPoolGamblerBetModel())
            case .failure(let error):
                if case BetError.forbidden = error {
                    currentPoolGamblerBet.lock()
                    state = .initial(currentPoolGamblerBet)
                } else {
                    state = .failure(
                        current: currentPoolGamblerBet,
                        failed: targetPoolGamblerBet,
                        error: error.mapOrDefaultLocalized { error in error.toBetLocalizedError() })
                }
            }
        }
    }
    
    @MainActor
    func reset() {
        state = .initial(state.relevantValue())
    }
}
