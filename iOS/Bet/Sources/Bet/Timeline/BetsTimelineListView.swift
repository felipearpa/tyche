import SwiftUI

public struct BetsTimelineListView: View {
    @StateObject private var viewModel: BetsTimelineViewModel
    private let onMatchOpen: MatchOpenHandler?

    public init(
        viewModel: @autoclosure @escaping () -> BetsTimelineViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        BetsTimelineList(
            lazyPagingItems: viewModel.lazyPager,
            onMatchOpen: onMatchOpen
        )
        .refreshable { viewModel.refresh() }
        .onAppearOnce { viewModel.refresh() }
    }
}
