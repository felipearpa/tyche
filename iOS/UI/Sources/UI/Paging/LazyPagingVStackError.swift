import SwiftUI

/// The full-list load error: the error message with a Retry action beneath it, centered in the
/// visible height left below `header`. `retry` reloads the list; pull to refresh stays available
/// alongside it.
///
/// Pass the list's `refresh()`, not `retry()`: this error is shown only after a failed refresh,
/// while `LazyPagingItems.retry()` replays the last load attempted, which can be an append that
/// was skipped during that refresh and then does nothing.
///
/// `header` is list content that stays above the error, such as a screen's identity or totals.
/// Pass it here instead of as a separate row, so the error is centered below it. Without a
/// header, the error is centered in the whole visible height. The visible height comes from
/// `fullListStateViewport(contentInsets:)` on the paging stack.
public struct LazyPagingVStackError<Header: View>: View {
    private let localizedError: LocalizedError
    private let retry: () -> Void
    private let header: Header

    @Environment(\.fullListStateHeight) private var fullListStateHeight
    @Environment(\.boxSpacing) private var boxSpacing

    public init(localizedError: LocalizedError, retry: @escaping () -> Void, @ViewBuilder header: () -> Header) {
        self.localizedError = localizedError
        self.retry = retry
        self.header = header()
    }

    public var body: some View {
        FullListStateLayout(minHeight: fullListStateHeight) {
            header

            VStack(spacing: boxSpacing.medium) {
                ErrorView(localizedError: localizedError)
                Button(action: retry, label: {
                    Text(.retryAction)
                })
                .buttonStyle(.standardProminent)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

public extension LazyPagingVStackError where Header == EmptyView {
    init(localizedError: LocalizedError, retry: @escaping () -> Void) {
        self.init(localizedError: localizedError, retry: retry, header: { EmptyView() })
    }
}

#Preview {
    LazyPagingVStackError(localizedError: UnknownLocalizedError(), retry: {})
}
