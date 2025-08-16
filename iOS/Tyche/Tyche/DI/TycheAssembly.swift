import Swinject
import Session

public class TycheAssembly : Assembly {
    public init() {}
    
    public func assemble(container: Swinject.Container) {
        container.register(SignInLinkUrlTemplateProvider.self) { _ in
            LocalSignInLinkUrlTemplateProvider()
        }
    }
}
