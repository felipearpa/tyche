import SwiftUI

public struct FinishedPoolGamblerBetListView: View {
    @StateObject private var viewModel: FinishedBetListViewModel
    private let onMatchOpen: MatchOpenHandler?

    public init(
        viewModel: @autoclosure @escaping () -> FinishedBetListViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onMatchOpen = onMatchOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        FinishedPoolGamblerBetList(
            lazyPagingItems: viewModel.lazyPager,
            onMatchOpen: onMatchOpen
        )
        .refreshable { viewModel.refresh() }
        .onAppearOnce { viewModel.refresh() }
    }
}
