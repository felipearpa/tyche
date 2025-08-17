import Swinject
import FirebaseAuth
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
            AuthenticatedSession(authTokenRetriever: resolver.resolve(AuthTokenRetriever.self)!)
        }
        
        container.register(Auth.self) { _ in
            Auth.auth()
        }
        
        container.register(AuthenticationExternalDataSource.self) { resolver in
            AuthenticationFirebaseDataSource(
                firebaseAuth: resolver.resolve(Auth.self)!,
                signInUrlTemplate: resolver.resolve(SignInLinkUrlTemplateProvider.self)!,
                androidPackageName: resolver.resolve(AndroidPackageNameProvider.self)!,
            )
        }
        
        container.register(AuthenticationRemoteDataSource.self) { resolver in
            AuthenticationAlamofireDataSource(
                urlBasePathProvider: resolver.resolve(URLBasePathProvider.self)!,
                session: resolver.resolve(AuthenticatedSession.self)!
            )
        }
        
        container.register(AuthenticationRepository.self) { resolver in
            AuthenticationRemoteRepository(
                authenticationExternalDataSource: resolver.resolve(AuthenticationExternalDataSource.self)!,
                authenticationRemoteDataSource: resolver.resolve(AuthenticationRemoteDataSource.self)!,
                networkErrorHandler: resolver.resolve(NetworkErrorHandler.self)!
            )
        }
        
        container.register(SendSignInLinkToEmailUseCase.self) { resolver in
            SendSignInLinkToEmailUseCase(
                authenticationRepository: resolver.resolve(AuthenticationRepository.self)!
            )
        }
        
        container.register(SignInWithEmailLinkUseCase.self) { resolver in
            SignInWithEmailLinkUseCase(
                authenticationRepository: resolver.resolve(AuthenticationRepository.self)!,
                accountStorage: resolver.resolve(AccountStorage.self)!
            )
        }

        container.register(SignInWithEmailAndPasswordUseCase.self) { resolver in
            SignInWithEmailAndPasswordUseCase(
                authenticationRepository: resolver.resolve(AuthenticationRepository.self)!,
                accountStorage: resolver.resolve(AccountStorage.self)!
            )
        }

        container.register(LogOutUseCase.self) { resolver in
            LogOutUseCase(
                authenticationRepository: resolver.resolve(AuthenticationRepository.self)!,
                accountStorage: resolver.resolve(AccountStorage.self)!
            )
        }
        
        container.register(AuthTokenRetriever.self) { resolver in
            AuthTokenFirebaseRetriever(firebaseAuth: resolver.resolve(Auth.self)!)
        }
    }
}
