import SwiftUI

/// A swipe-to-dismiss container mirroring the Android Material3 `SwipeToDismissBox` API shape.
///
/// The `content` row translates horizontally as the user drags over the `background` slot. Releasing
/// past `thresholdFraction` of the row's width invokes `onConfirm` (e.g. to open a confirmation
/// prompt) — it never performs the destructive action directly — and then animates the row back to
/// rest. Releasing under the threshold snaps back with no side effect.
///
/// The `background` builder receives the drag `progress` (0 at rest, 1 at the threshold) and whether
/// the swipe is currently `isPastThreshold`, so the destructive affordance can track the gesture and
/// react once the commit threshold is crossed.
public struct SwipeToDismissBox<Background: View, Content: View>: View {
    private let thresholdFraction: CGFloat
    private let onConfirm: () -> Void
    private let background: (_ progress: Double, _ isPastThreshold: Bool) -> Background
    private let content: () -> Content

    @State private var offsetX: CGFloat = 0
    @State private var width: CGFloat = 0

    public init(
        thresholdFraction: CGFloat = 0.35,
        onConfirm: @escaping () -> Void,
        @ViewBuilder background: @escaping (_ progress: Double, _ isPastThreshold: Bool) -> Background,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.thresholdFraction = thresholdFraction
        self.onConfirm = onConfirm
        self.background = background
        self.content = content
    }

    private var threshold: CGFloat {
        max(width * thresholdFraction, 1)
    }

    private var progress: Double {
        guard width > 0 else { return 0 }
        return min(Double(-offsetX / threshold), 2)
    }

    private var isPastThreshold: Bool {
        -offsetX >= threshold
    }

    public var body: some View {
        ZStack {
            background(progress, isPastThreshold)

            content()
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .onAppear { width = proxy.size.width }
                            .onChange(of: proxy.size.width) { newWidth in width = newWidth }
                    }
                )
                .offset(x: offsetX)
                .gesture(dragGesture)
        }
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 12)
            .onChanged { value in
                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                offsetX = max(min(value.translation.width, 0), -width)
            }
            .onEnded { _ in
                let crossed = isPastThreshold
                withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) {
                    offsetX = 0
                }
                if crossed {
                    onConfirm()
                }
            }
    }
}
