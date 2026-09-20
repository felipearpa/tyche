import SwiftUI
import Shimmer

public extension View {
    func shimmer() -> some View {
        self.redacted(reason: .placeholder)
            .shimmering(
                animation: Animation
                    .linear(duration: 1.5)
                    .delay(0.25)
                    .repeatForever(autoreverses: true)
            )
    }
}

/// The shared shimmer treatment as a passable value, for components that take
/// a placeholder modifier instead of applying `.shimmer()` themselves.
public struct ShimmerModifier: ViewModifier {
    public init() {}

    public func body(content: Content) -> some View {
        content.shimmer()
    }
}
