import Swinject
import Session
import Core

public class TycheAssembly : Assembly {
    public init() {}
    
    public func assemble(container: Swinject.Container) {
        container.register(URLBasePathProvider.self) { _ in
            LocalURLBasePathProvider()
        }

        container.register(SignInLinkUrlTemplateProvider.self) { _ in
            LocalSignInLinkUrlTemplateProvider()
        }

        container.register(AndroidPackageNameProvider.self) { _ in
            LocalAndroidPackageNameProvider()
        }
    }
}
