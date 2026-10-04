import Swinject
import Core
import DataBet
import DataPool

func diFakeResolver() -> DIResolver {
    let container = Container()
    container.register(PoolGamblerBetRepository.self) { _ in
        PoolGamblerBetFakeRepository()
    }
    container.register(BetUseCase.self) { _ in
        BetUseCase(poolGamblerBetRepository: PoolGamblerBetFakeRepository())
    }
    container.register(GetPoolGamblerScoreUseCase.self) { _ in
        GetPoolGamblerScoreUseCase(poolGamblerScoreRepository: PoolGamblerScoreFakeRepository())
    }
    return DIResolver(resolver: container.synchronize())
}
