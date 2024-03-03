import SwiftUI

public let LocalBoxSpacing = BoxSpacing(small: 4, medium: 8, large: 16)

public struct BoxSpacingKey: EnvironmentKey {
    public static let defaultValue: BoxSpacing = LocalBoxSpacing
}

public extension EnvironmentValues {
    var boxSpacing: BoxSpacing {
        get { self[BoxSpacingKey.self] }
        set { self[BoxSpacingKey.self] = newValue }
    }
}
