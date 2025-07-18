import Swinject
import Core
import Session

public class PoolAssembly : Assembly {
    public init() {}
    
    public func assemble(container: Container) {
        container.register(PoolGamblerScoreRemoteDataSource.self) { resolver in
            PoolGamblerScoreAlamofireDateSource(
                urlBasePathProvider: resolver.resolve(URLBasePathProvider.self)!,
                session: resolver.resolve(AuthenticatedSession.self)!
            )
        }

        container.register(PoolRemoteDataSource.self) { resolver in
            PoolAlamofireDataSource(
                urlBasePathProvider: resolver.resolve(URLBasePathProvider.self)!,
                session: resolver.resolve(AuthenticatedSession.self)!
            )
        }

        container.register(PoolLayoutRemoteDataSource.self) { resolver in
            PoolLayoutAlamofireDataSource(
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

        container.register(PoolRepository.self) { resolver in
            PoolRemoteRepository(
                poolRemoteDataSource: resolver.resolve(PoolRemoteDataSource.self)!,
                networkErrorHandler: resolver.resolve(NetworkErrorHandler.self)!
            )
        }

        container.register(PoolLayoutRepository.self) { resolver in
            PoolLayoutRemoteRepository(
                poolLayoutDataSource: resolver.resolve(PoolLayoutRemoteDataSource.self)!,
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
        
        container.register(GetPoolGamblerScoreUseCase.self) { resolver in
            GetPoolGamblerScoreUseCase(
                poolGamblerScoreRepository: resolver.resolve(PoolGamblerScoreRepository.self)!
            )
        }

        container.register(GetPoolUseCase.self) { resolver in
            GetPoolUseCase(
                poolRepository: resolver.resolve(PoolRepository.self)!
            )
        }

        container.register(CreatePoolUseCase.self) { resolver in
            CreatePoolUseCase(
                poolRepository: resolver.resolve(PoolRepository.self)!
            )
        }

        container.register(JoinPoolUseCase.self) { resolver in
            JoinPoolUseCase(
                poolRepository: resolver.resolve(PoolRepository.self)!
            )
        }

        container.register(GetOpenPoolLayoutsUseCase.self) { resolver in
            GetOpenPoolLayoutsUseCase(
                poolLayoutRepository: resolver.resolve(PoolLayoutRepository.self)!
            )
        }

        container.register(PoolStorage.self) { _ in
            PoolStorageInUserDefaults()
        }
    }
}
