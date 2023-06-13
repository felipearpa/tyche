import Foundation
import Swinject

public class DIResolver : ObservableObject {
    let resolver: Resolver
    
    public init(resolver: Resolver) {
        self.resolver = resolver
    }
    
    public func resolve<T>(_ serviceType: T.Type) -> T? {
        return self.resolver.resolve(serviceType)
    }
}
