import SwiftUI

private struct Shimmer: ViewModifier {
    static let defaultAnimation = Animation.linear(duration: 1.5).repeatForever(autoreverses: false)
    
    let animation: Animation
    @State private var phase: CGFloat = 0
    
    init(animation: Animation = Self.defaultAnimation) {
        self.animation = animation
    }
    
    func body(content: Content) -> some View {
        content
            .modifier(
                AnimatedMask(phase: phase).animation(animation)
            )
            .onAppear { phase = 0.8 }
    }
    
    struct AnimatedMask: AnimatableModifier {
        var phase: CGFloat = 0
        
        var animatableData: CGFloat {
            get { phase }
            set { phase = newValue }
        }
        
        func body(content: Content) -> some View {
            content
                .mask(GradientMask(phase: phase).scaleEffect(3))
        }
    }
    
    struct GradientMask: View {
        let phase: CGFloat
        let centerColor = Color.black
        let edgeColor = Color.black.opacity(0.3)
        @Environment(\.layoutDirection) private var layoutDirection
        
        var body: some View {
            let isRightToLeft = layoutDirection == .rightToLeft
            LinearGradient(
                gradient: Gradient(stops: [
                    .init(color: edgeColor, location: phase),
                    .init(color: centerColor, location: phase + 0.1),
                    .init(color: edgeColor, location: phase + 0.2)
                ]),
                startPoint: isRightToLeft ? .bottomTrailing : .topLeading,
                endPoint: isRightToLeft ? .topLeading : .bottomTrailing
            )
        }
    }
}

public extension View {
    @ViewBuilder func shimmer() -> some View {
        foregroundColor(ColorScheme.shimmer.color)
            .redacted(reason: .placeholder)
            .modifier(Shimmer())
    }
}

