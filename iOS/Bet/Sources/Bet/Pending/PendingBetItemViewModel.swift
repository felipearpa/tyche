import Foundation
import UI
import Core
import DataBet
import ViewingState

class PendingBetItemViewModel: ObservableObject {
    private let betUseCase: BetUseCase

    @Published @MainActor private(set) var state: MutationState<PoolGamblerBetModel>?

    @MainActor
    public init(betUseCase: BetUseCase) {
        self.betUseCase = betUseCase
        self.state = nil
    }

    @MainActor
    func bind(_ poolGamblerBet: PoolGamblerBetModel) {
        switch state {
        case nil, .idle:
            state = .idle(poolGamblerBet)
        case .mutated(let old, _):
            state = .mutated(original: old, updated: poolGamblerBet)
        case .mutating, .failure:
            break
        }
    }

    @MainActor
    func retryBet() {
        guard case .failure(_, updated: let poolGamblerBet, _) = state else { return }
        guard let betScore = poolGamblerBet.betScore else { return }
        bet(betScore: betScore)
    }

    @MainActor
    func bet(betScore: TeamScore<Int>) {
        Task { [self] in
            guard let currentState = state else { return }
            var currentPoolGamblerBet = currentState.activeValue()

            let targetPoolGamblerBet = currentPoolGamblerBet.copy { builder in
                builder.betScore = betScore
            }

            state = .mutating(original: currentPoolGamblerBet, updated: targetPoolGamblerBet)

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
                state = .mutated(
                    original: currentPoolGamblerBet,
                    updated: updatedPoolGamblerBet.toPoolGamblerBetModel()
                )
            case .failure(let error):
                if case BetError.forbidden = error {
                    currentPoolGamblerBet.lock()
                    state = .idle(currentPoolGamblerBet)
                } else {
                    state = .failure(
                        original: currentPoolGamblerBet,
                        updated: targetPoolGamblerBet,
                        error: error.mapOrDefaultLocalized { error in error.asBetLocalizedError() }
                    )
                }
            }
        }
    }

    @MainActor
    func reset() {
        guard let currentState = state else { return }
        state = .idle(currentState.activeValue())
    }
}
