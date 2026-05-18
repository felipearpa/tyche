import SwiftUI

public let LocalBoxSpacing = BoxSpacing(
    small: 4,
    medium: 8,
    large: 16,
    extraLarge: 24,
    xxLarge: 32,
    xxxLarge: 40,
    huge: 48,
    massive: 64
)

public struct BoxSpacingKey: EnvironmentKey {
    public static let defaultValue: BoxSpacing = LocalBoxSpacing
}

public extension EnvironmentValues {
    var boxSpacing: BoxSpacing {
        get { self[BoxSpacingKey.self] }
        set { self[BoxSpacingKey.self] = newValue }
    }
}
