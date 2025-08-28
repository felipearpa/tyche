import SwiftUI

public struct StatefulLazyVStackError: View {
    let localizedError: LocalizedError

    @Environment(\.parentSize) private var parentSize
    @Environment(\.parentSafeAreaInsets) private var parentSafeAreaInsets

    public init(localizedError: LocalizedError) {
        self.localizedError = localizedError
    }

    public var body: some View {
        ErrorView(localizedError: localizedError)
            .frame(minHeight: parentSize.height - parentSafeAreaInsets.top - parentSafeAreaInsets.bottom)
    }
}

#Preview {
    StatefulLazyVStackError(localizedError: UnknownLocalizedError())
}
