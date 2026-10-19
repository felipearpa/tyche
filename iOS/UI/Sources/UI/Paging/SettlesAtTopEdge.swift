import SwiftUI

public extension View {
    /// Returns the scrollable view within this view to its top edge when a scroll comes to rest
    /// past it.
    ///
    /// A `LazyVStack` can change the scroll view's content size as the refresh control retracts;
    /// that stops the retraction, which is never reported as finished, and leaves the list
    /// overscrolled with a navigation large title lowered until the next scroll. On iOS 18 and
    /// later, a scroll that still rests past the top edge shortly after it should have settled is
    /// scrolled back; earlier systems are unchanged.
    func settlesAtTopEdge() -> some View {
        modifier(SettlesAtTopEdge())
    }
}

private struct SettlesAtTopEdge: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 18, *) {
            content.modifier(ScrollsBackToTopEdge())
        } else {
            content
        }
    }
}

@available(iOS 18, *)
private struct ScrollsBackToTopEdge: ViewModifier {
    @State private var position = ScrollPosition()
    @State private var topEdge = TopEdge(offset: 0, inset: 0)
    @State private var settling: Task<Void, Never>?

    func body(content: Content) -> some View {
        content
            .scrollPosition($position)
            .onScrollGeometryChange(for: TopEdge.self, of: TopEdge.init) { _, topEdge in
                self.topEdge = topEdge
            }
            .onScrollPhaseChange { _, phase, context in
                settling?.cancel()
                guard phase != .interacting, TopEdge(context.geometry).restsPast else { return }
                settling = Task {
                    try? await Task.sleep(for: settlingDelay)
                    guard !Task.isCancelled, topEdge.restsPast else { return }
                    withAnimation { position.scrollTo(edge: .top) }
                }
            }
    }
}

/// Time a retracting refresh control needs to bring the content back to the top edge.
private let settlingDelay = Duration.milliseconds(500)

@available(iOS 18, *)
private struct TopEdge: Equatable {
    let offset: CGFloat
    let inset: CGFloat

    init(offset: CGFloat, inset: CGFloat) {
        self.offset = offset
        self.inset = inset
    }

    init(_ geometry: ScrollGeometry) {
        self.init(offset: geometry.contentOffset.y, inset: geometry.contentInsets.top)
    }

    var restsPast: Bool {
        offset < -inset - 0.5
    }
}
