import Foundation
import UI
import Core

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
        if case .failure(_, failed: let poolGamblerBet, _) = state {
            guard let betScore = poolGamblerBet.betScore else { return }
            bet(betScore: betScore)
        }
    }
    
    @MainActor
    func bet(betScore: TeamScore<Int>) {
        Task { [self] in
            var currentPoolGamblerBet = state.value()
            
            let prePoolGamblerBet = currentPoolGamblerBet.copy { builder in
                builder.betScore = betScore
            }
            
            state = .loading(current: currentPoolGamblerBet, target: prePoolGamblerBet)
            
            let bet = Bet(
                poolId: prePoolGamblerBet.poolId,
                gamblerId: prePoolGamblerBet.gamblerId,
                matchId: prePoolGamblerBet.matchId,
                homeTeamBet: BetScore(betScore.homeTeamValue)!,
                awayTeamBet: BetScore(betScore.awayTeamValue)!
            )
            let result = await betUseCase.execute(bet: bet)
            
            switch result {
            case .success(let updatedPoolGamblerBet):
                state = .success(current: currentPoolGamblerBet, succeeded: updatedPoolGamblerBet.toPoolGamblerBetModel())
            case .failure(let error):
                if case BetError.forbidden = error {
                    currentPoolGamblerBet.lock()
                    state = .initial(currentPoolGamblerBet)
                } else {
                    state = .failure(
                        current: currentPoolGamblerBet,
                        failed: prePoolGamblerBet,
                        error: error.toLocalizedError(transformer: { error in error.toBetLocalizedError() }))
                }
            }
        }
    }
    
    @MainActor
    func reset() {
        state = .initial(state.value())
    }
}
