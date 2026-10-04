import SwiftUI

/// The empty-list message, centered in the visible height left below `header`.
///
/// `header` is list content that stays above the message, such as a screen's identity or totals.
/// Pass it here instead of as a separate row, so the message is centered below it. Without a
/// header, the message is centered in the whole visible height. The visible height comes from
/// `fullListStateViewport(contentInsets:)` on the paging stack.
public struct LazyPagingVStackEmpty<Header: View>: View {
    private let header: Header

    @Environment(\.fullListStateHeight) private var fullListStateHeight

    public init(@ViewBuilder header: () -> Header) {
        self.header = header()
    }

    public var body: some View {
        FullListStateLayout(minHeight: fullListStateHeight) {
            header

            MessageView(
                icon: Image(.search),
                message: String(localized: .emptyListMessage)
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

public extension LazyPagingVStackEmpty where Header == EmptyView {
    init() {
        self.init(header: { EmptyView() })
    }
}

#Preview {
    LazyPagingVStackEmpty()
}
