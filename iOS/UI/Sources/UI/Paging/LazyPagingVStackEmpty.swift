import SwiftUI

public struct LazyPagingVStackEmpty: View {
    @Environment(\.parentSize) private var parentSize
    @Environment(\.parentSafeAreaInsets) private var parentSafeAreaInsets

    public init() {}

    public var body: some View {
        MessageView(
            icon: Image(.search),
            message: String(localized: .emptyListMessage)
        )
        .frame(minHeight: parentSize.height - parentSafeAreaInsets.top - parentSafeAreaInsets.bottom)
    }
}

#Preview {
    LazyPagingVStackEmpty()
}
