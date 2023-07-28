import Swinject
import Core
import User

public class PoolAssembly : Assembly {

    public init() {}
    
    public func assemble(container: Container) {
        container.register(PoolGamblerScoreRemoteDataSource.self) { resolver in
            PoolGamblerScoreAlamofireDateSource(
                urlBasePathProvider: resolver.resolve(URLBasePathProvider.self)!,
                session: resolver.resolve(AuthenticatedSession.self)!
            )
        }
        
        container.register(PoolGamblerScoreRepository.self) { resolver in
            PoolGamblerScoreRemoteRepository(
                poolGamblerScoreRemoteDataSource: resolver.resolve(PoolGamblerScoreRemoteDataSource.self)!,
                networkErrorHandler: resolver.resolve(NetworkErrorHandler.self)!
            )
        }
        
        container.register(GetPoolGamblerScoresByGamblerUseCase.self) { resolver in
            GetPoolGamblerScoresByGamblerUseCase(
                poolGamblerScoreRepository: resolver.resolve(PoolGamblerScoreRepository.self)!
            )
        }
        
        container.register(GetPoolGamblerScoresByPoolUseCase.self) { resolver in
            GetPoolGamblerScoresByPoolUseCase(
                poolGamblerScoreRepository: resolver.resolve(PoolGamblerScoreRepository.self)!
            )
        }
    }
}
