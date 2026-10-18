import SwiftUI

/// How far a `SwipeToDismissBox` row must be dragged before releasing it confirms the swipe.
public enum SwipeToDismissThreshold: Equatable, Sendable {
    /// A fraction of the row's width, so wider rows need a longer drag.
    case fraction(CGFloat)
    /// A fixed drag distance in points, independent of the row's width.
    case distance(CGFloat)

    /// The drag distance, in points, at which a row of `rowWidth` confirms. Never less than 1 pt.
    func distance(forRowWidth rowWidth: CGFloat) -> CGFloat {
        switch self {
        case .fraction(let fraction):
            return max(rowWidth * fraction, 1)
        case .distance(let distance):
            return max(distance, 1)
        }
    }

    /// Whether releasing after dragging `dragDistance` points on a row of `rowWidth` confirms.
    func isCrossed(byDragDistance dragDistance: CGFloat, rowWidth: CGFloat) -> Bool {
        dragDistance >= distance(forRowWidth: rowWidth)
    }
}

/// A swipe-to-dismiss container mirroring the Android Material3 `SwipeToDismissBox` API shape.
///
/// The `content` row translates horizontally as the user drags over the `background` slot. Releasing
/// past `threshold` (by default 35 % of the row's width) invokes `onConfirm` (e.g. to open a confirmation
/// prompt) — it never performs the destructive action directly — and then animates the row back to
/// rest. Releasing under the threshold snaps back with no side effect.
///
/// The `background` builder receives the drag `progress` (0 at rest, 1 at the threshold) and whether
/// the swipe is currently `isPastThreshold`, so the destructive affordance can track the gesture and
/// react once the commit threshold is crossed.
public struct SwipeToDismissBox<Background: View, Content: View>: View {
    private let thresholdRule: SwipeToDismissThreshold
    private let isEnabled: Bool
    private let onConfirm: () -> Void
    private let background: (_ progress: Double, _ isPastThreshold: Bool) -> Background
    private let content: () -> Content

    @State private var offsetX: CGFloat = 0
    @State private var width: CGFloat = 0

    public init(
        threshold: SwipeToDismissThreshold = .fraction(0.35),
        isEnabled: Bool = true,
        onConfirm: @escaping () -> Void,
        @ViewBuilder background: @escaping (_ progress: Double, _ isPastThreshold: Bool) -> Background,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.thresholdRule = threshold
        self.isEnabled = isEnabled
        self.onConfirm = onConfirm
        self.background = background
        self.content = content
    }

    private var threshold: CGFloat {
        thresholdRule.distance(forRowWidth: width)
    }

    private var progress: Double {
        guard width > 0 else { return 0 }
        return min(Double(-offsetX / threshold), 2)
    }

    private var isPastThreshold: Bool {
        thresholdRule.isCrossed(byDragDistance: -offsetX, rowWidth: width)
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
                .gesture(dragGesture, including: isEnabled ? .all : .subviews)
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
