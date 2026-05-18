import SwiftUI

public struct LazyPagingVStackConcatenateError: View {
    private let localizedError: LocalizedError
    private let retry: () -> Void
    @Environment(\.boxSpacing) private var boxSpacing

    public init(localizedError: LocalizedError, retry: @escaping () -> Void) {
        self.localizedError = localizedError
        self.retry = retry
    }

    public var body: some View {
        VStack(spacing: boxSpacing.medium) {
            ErrorView(localizedError: localizedError)
            Button(action: retry, label: {
                Text(.retryAction)
            })
            .buttonStyle(.liquidGlassProminent)
        }
    }
}

#Preview {
    LazyPagingVStackConcatenateError(localizedError: UnknownLocalizedError(), retry: {})
}
