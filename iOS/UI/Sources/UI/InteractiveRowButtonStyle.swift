import SwiftUI

/// Native semantic row activation with a quiet, theme-aware pressed state.
/// Use this style for full-width list rows that navigate when activated.
public struct InteractiveRowButtonStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())
            .overlay {
                Color(sharedResource: .onSurface)
                    .opacity(configuration.isPressed ? pressedStateOpacity : 0)
                    .allowsHitTesting(false)
            }
    }
}

private let pressedStateOpacity = 0.08
