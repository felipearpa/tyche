import SwiftUI

struct ScrollViewSwipeActionsModifier: ViewModifier {
    @State private var size: CGSize = .init(width: 1, height: 1)

    func body(content: Content) -> some View {
        List {
            LazyVStack {
                content
            }
            .frame(minHeight: 44)
            .readSize { size in
                self.size = size
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
        }
        .scrollDisabled(true)
        .listStyle(.plain)
        .frame(height: size.height)
    }
}

public extension View {
    func enableViewSwipeActions() -> some View {
        modifier(ScrollViewSwipeActionsModifier())
    }
}
