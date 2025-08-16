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
