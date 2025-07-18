import SwiftUI

public struct FinishedPoolGamblerBetListView: View {
    @StateObject private var viewModel: FinishedBetListViewModel

    public init(viewModel: @autoclosure @escaping () -> FinishedBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        let _ = Self._printChanges()
        
        FinishedPoolGamblerBetList(lazyPager: viewModel.lazyPager)
            .refreshable { viewModel.refresh() }
            .onAppearOnce { viewModel.load() }
    }
}
