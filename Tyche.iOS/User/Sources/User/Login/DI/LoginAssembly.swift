import Swinject
import Core

public class LoginAssembly : Assembly {

    public init() {}
    
    public func assemble(container: Swinject.Container) {
        container.register(LoginRemoteDataSource.self) { resolver in
            AlamofireLoginRemoteDataSource(
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
                loginRepository: resolver.resolve(LoginRemoteRepository.self)!
            )
        }
    }
}
