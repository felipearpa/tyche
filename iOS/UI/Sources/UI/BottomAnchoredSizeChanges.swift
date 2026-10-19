import SwiftUI

public extension View {
    /// When this scroll view's content changes size, keeps its bottom edge in place instead of its
    /// top, so content shrinking at the end of the list slides up rather than clamping the scroll
    /// offset in one frame. It applies only to size changes: the scroll view still opens at the
    /// top. Available from iOS 18; earlier versions keep the system behavior.
    func keepsBottomAnchoredOnSizeChanges() -> some View {
        modifier(BottomAnchoredSizeChanges())
    }
}

private struct BottomAnchoredSizeChanges: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 18, *) {
            content.defaultScrollAnchor(.bottom, for: .sizeChanges)
        } else {
            content
        }
    }
}
