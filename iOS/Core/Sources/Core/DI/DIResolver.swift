import Foundation
import Swinject

public class DIResolver {
    let resolver: Resolver
    
    public init(resolver: Resolver) {
        self.resolver = resolver
    }
    
    public func resolve<T>(_ serviceType: T.Type) -> T? {
        self.resolver.resolve(serviceType)
    }
}
