import SwiftUI

/// Stacks the list content shown above a full-list state, then gives the state the rest of
/// `minHeight`, so the state is centered in the visible space below that content rather than
/// in a full viewport that would run past the bottom edge.
///
/// The last subview is the state; the others are stacked above it, each centered horizontally as
/// a `LazyVStack` row is. `minHeight` is a minimum: when the content above and the state's ideal
/// height together exceed it, the layout grows and the list scrolls, so nothing is clipped.
struct FullListStateLayout: Layout {
    let minHeight: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard let state = subviews.last else { return .zero }
        let width = proposal.width ?? subviews.map { $0.sizeThatFits(.unspecified).width }.max() ?? 0
        let contentAbove = heightAbove(subviews: subviews, width: width)
        let stateHeight = state.sizeThatFits(ProposedViewSize(width: width, height: nil)).height
        return CGSize(width: width, height: contentAbove + max(stateHeight, minHeight - contentAbove))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard let state = subviews.last else { return }
        var top = bounds.minY
        for subview in subviews.dropLast() {
            let height = subview.sizeThatFits(ProposedViewSize(width: bounds.width, height: nil)).height
            subview.place(
                at: CGPoint(x: bounds.midX, y: top),
                anchor: .top,
                proposal: ProposedViewSize(width: bounds.width, height: height)
            )
            top += height
        }
        state.place(
            at: CGPoint(x: bounds.minX, y: top),
            anchor: .topLeading,
            proposal: ProposedViewSize(width: bounds.width, height: bounds.maxY - top)
        )
    }

    private func heightAbove(subviews: Subviews, width: CGFloat) -> CGFloat {
        subviews.dropLast().reduce(0) { height, subview in
            height + subview.sizeThatFits(ProposedViewSize(width: width, height: nil)).height
        }
    }
}

private struct FullListStateHeightKey: EnvironmentKey {
    static let defaultValue: CGFloat = 0
}

extension EnvironmentValues {
    /// The height a full-list state and the content above it fill: the visible height of the
    /// list's scroll view less its content insets, as `fullListStateViewport(contentInsets:)`
    /// measures it. Zero, so no minimum, in a list that does not measure it.
    var fullListStateHeight: CGFloat {
        get { self[FullListStateHeightKey.self] }
        set { self[FullListStateHeightKey.self] = newValue }
    }
}

public extension View {
    /// Measures the visible height of this paging list, between its navigation bar and tab bar
    /// or other safe-area edges, so its full-list error and empty states
    /// (`LazyPagingVStackError`, `LazyPagingVStackEmpty`) are centered in the visible space below
    /// any content above them.
    ///
    /// Apply it to the paging stack and pass the same `contentInsets` the stack was given: they
    /// take part of the visible height before the state does.
    func fullListStateViewport(contentInsets: EdgeInsets = EdgeInsets()) -> some View {
        modifier(FullListStateViewport(contentInsets: contentInsets))
    }
}

private struct FullListStateViewport: ViewModifier {
    let contentInsets: EdgeInsets

    @State private var visibleHeight: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .environment(\.fullListStateHeight, max(0, visibleHeight - contentInsets.top - contentInsets.bottom))
            .background(
                // A geometry reader's size already excludes the safe areas the scroll view extends
                // under (bars, home indicator), so it is the visible height.
                GeometryReader { proxy in
                    Color.clear
                        .onAppear { visibleHeight = proxy.size.height }
                        .onChange(of: proxy.size.height) { height in
                            visibleHeight = height
                        }
                }
            )
    }
}
