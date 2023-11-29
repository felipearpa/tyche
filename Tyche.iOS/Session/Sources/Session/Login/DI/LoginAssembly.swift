import Swinject
import Core

private let AUTH_SUITE = "auth"
private let AUTH_KEY = "auth"

private let ACCOUNT_SUITE = "account"
private let ACCOUNT_KEY = "account"

public class LoginAssembly : Assembly {

    public init() {}
    
    public func assemble(container: Swinject.Container) {
        container.register(AuthStorage.self) { _ in
            AuthStorageInUserDefaults(
                storageInUserDefaults: StorageInUserDefaults(key: AUTH_KEY, suite: AUTH_SUITE)
            )
        }.inObjectScope(.container)
        
        container.register(AccountStorage.self) { _ in
            AccountStorageInUserDefaults(
                storageInUserDefaults: StorageInUserDefaults(key: ACCOUNT_KEY, suite: ACCOUNT_SUITE)
            )
        }.inObjectScope(.container)
        
        container.register(AuthenticatedSession.self) { resolver in
            AuthenticatedSession(authStorage: resolver.resolve(AuthStorage.self)!)
        }
        
        container.register(LoginRemoteDataSource.self) { resolver in
            LoginAlamofireDataSource(
                urlBasePathProvider: resolver.resolve(URLBasePathProvider.self)!
            )
        }
        
        container.register(LoginRepository.self) { resolver in
            LoginRemoteRepository(
                loginRemoteDataSource: resolver.resolve(LoginRemoteDataSource.self)!,
                networkErrorHandler: resolver.resolve(NetworkErrorHandler.self)!
            )
        }
        
        container.register(LoginUseCase.self) { resolver in
            LoginUseCase(
                loginRepository: resolver.resolve(LoginRemoteRepository.self)!,
                authStorage: resolver.resolve(AuthStorage.self)!,
                accountStorage: resolver.resolve(AccountStorage.self)!
            )
        }
    }
}
