import Swinject

public class CoreAssembly : Assembly {
    public init() {}
    
    public func assemble(container: Swinject.Container) {
        container.register(NetworkErrorHandler.self) { _ in
            AlamofireErrorHandler()
        }
    }
}
