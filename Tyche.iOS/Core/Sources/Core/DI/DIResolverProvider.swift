import SwiftUI
import Swinject

private let localDIResolver = DIResolver(resolver: Assembler([]).resolver)

public struct DIResolverKey: EnvironmentKey {
    public static let defaultValue: DIResolver = localDIResolver
}

public extension EnvironmentValues {
    var diResolver: DIResolver {
        get { self[DIResolverKey.self] }
        set { self[DIResolverKey.self] = newValue }
    }
}
