import Swinject
import Core

func diFakeResolver() -> DIResolver {
    let container = Container()
    container.register(BetUseCase.self) { _ in
        BetUseCase(poolGamblerBetRepository: PoolGamblerBetFakeRepository())
    }
    return DIResolver(resolver: container.synchronize())
}
