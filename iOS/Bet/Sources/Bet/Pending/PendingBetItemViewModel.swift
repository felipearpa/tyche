import Foundation
import UI
import Core
import DataBet

class PendingBetItemViewModel: ObservableObject {
    private let betUseCase: BetUseCase

    @Published @MainActor private(set) var state: EditableViewState<PoolGamblerBetModel>?

    @MainActor
    public init(betUseCase: BetUseCase) {
        self.betUseCase = betUseCase
        self.state = nil
    }

    @MainActor
    func bind(_ poolGamblerBet: PoolGamblerBetModel) {
        switch state {
        case nil, .initial:
            state = .initial(poolGamblerBet)
        case .success(let old, _):
            state = .success(old: old, succeeded: poolGamblerBet)
        case .saving, .failure:
            break
        }
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
            guard let currentState = state else { return }
            var currentPoolGamblerBet = currentState.relevantValue()

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
                state = .success(
                    old: currentPoolGamblerBet,
                    succeeded: updatedPoolGamblerBet.toPoolGamblerBetModel()
                )
            case .failure(let error):
                if case BetError.forbidden = error {
                    currentPoolGamblerBet.lock()
                    state = .initial(currentPoolGamblerBet)
                } else {
                    state = .failure(
                        current: currentPoolGamblerBet,
                        failed: targetPoolGamblerBet,
                        error: error.mapOrDefaultLocalized { error in error.asBetLocalizedError() }
                    )
                }
            }
        }
    }

    @MainActor
    func reset() {
        guard let currentState = state else { return }
        state = .initial(currentState.relevantValue())
    }
}
