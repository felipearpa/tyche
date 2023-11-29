import Swinject
import Core
import Session

public class BetAssembly: Assembly {

    public init() {}
    
    public func assemble(container: Container) {
        container.register(PoolGamblerBetRemoteDataSource.self) { resolver in
            PoolGamblerBetAlamofireDataSource(
                urlBasePathProvider: resolver.resolve(URLBasePathProvider.self)!,
                session: resolver.resolve(AuthenticatedSession.self)!
            )
        }
        
        container.register(PoolGamblerBetRepository.self) { resolver in
            PoolGamblerBetRemoteRepository(
                poolGamblerBetRemoteDataSource: resolver.resolve(PoolGamblerBetRemoteDataSource.self)!,
                networkErrorHandler: resolver.resolve(NetworkErrorHandler.self)!
            )
        }
        
        container.register(GetPoolGamblerBetsUseCase.self) { resolver in
            GetPoolGamblerBetsUseCase(
                poolGamblerBetRepository: resolver.resolve(PoolGamblerBetRepository.self)!
            )
        }
        
        container.register(BetUseCase.self) { resolver in
            BetUseCase(
                poolGamblerBetRepository: resolver.resolve(PoolGamblerBetRepository.self)!
            )
        }
    }
}
