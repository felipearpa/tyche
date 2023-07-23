import Swinject
import Core

public class LoginAssembly : Assembly {

    public init() {}
    
    public func assemble(container: Swinject.Container) {
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
                loginStorage: resolver.resolve(LoginStorage.self)!
            )
        }
        
        container.register(LoginStorage.self) { _ in
            LoginStorageInKeychain()
        }
        
        container.register(AuthenticatedSession.self) { resolver in
            AuthenticatedSession(loginStorage: resolver.resolve(LoginStorage.self)!)
        }
    }
}
