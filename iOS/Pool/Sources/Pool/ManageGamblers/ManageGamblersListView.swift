import SwiftUI
import Core
import UI
import DataPool

public struct ManageGamblersListView: View {
    @StateObject private var viewModel: ManageGamblersListViewModel

    @State private var gamblerPendingRemoval: PoolMemberModel?

    public init(
        viewModel: @autoclosure @escaping () -> ManageGamblersListViewModel
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
    }

    public var body: some View {
        ManageGamblersList(
            lazyPagingItems: viewModel.lazyPager,
            gamblerCount: viewModel.shownGamblerCount,
            rowHolder: { viewModel.rowHolders[$0.gamblerId] },
            isRemoved: { viewModel.removedGamblerIds.contains($0.gamblerId) },
            onRequestRemove: { gamblerPendingRemoval = $0 },
            onRefresh: { await viewModel.refresh() }
        )
        // The heading is the list's first row, so the bar carries only Back.
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            removalAlertTitle,
            isPresented: Binding(
                get: { gamblerPendingRemoval != nil },
                set: { isPresented in
                    if !isPresented {
                        gamblerPendingRemoval = nil
                    }
                }
            ),
            presenting: gamblerPendingRemoval
        ) { member in
            Button(String(localized: .cancelAction), role: .cancel) {
                gamblerPendingRemoval = nil
            }
            Button(String(localized: .removeGamblerAction), role: .destructive) {
                viewModel.remove(member)
                gamblerPendingRemoval = nil
            }
        } message: { _ in
            Text(.removeGamblerAlertMessage)
        }
    }

    private var removalAlertTitle: String {
        guard let member = gamblerPendingRemoval else {
            return ""
        }
        return String(localized: .removeGamblerAlertTitle("@\(member.gamblerUsername)"))
    }
}
