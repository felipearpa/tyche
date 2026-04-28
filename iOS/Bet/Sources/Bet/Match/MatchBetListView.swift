import SwiftUI

public struct MatchBetListView: View {
    @StateObject private var viewModel: MatchBetListViewModel

    public init(viewModel: @autoclosure @escaping () -> MatchBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        MatchBetList(lazyPagingItems: viewModel.lazyPager)
            .refreshable { viewModel.refresh() }
            .onAppearOnce { viewModel.refresh() }
    }
}
