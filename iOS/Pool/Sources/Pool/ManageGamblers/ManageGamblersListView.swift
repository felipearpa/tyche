import SwiftUI
import Core
import UI
import DataPool

public struct ManageGamblersListView: View {
    @StateObject private var viewModel: ManageGamblersListViewModel
    private let onInvite: () -> Void

    @State private var isEditing = false
    @State private var gamblerPendingRemoval: PoolMemberModel?

    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> ManageGamblersListViewModel,
        onInvite: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onInvite = onInvite
    }

    public var body: some View {
        ManageGamblersList(
            lazyPagingItems: viewModel.lazyPager,
            isEditing: isEditing,
            isDeleting: { viewModel.isDeleting($0) },
            isRemoved: { viewModel.isRemoved($0) },
            onRequestRemove: { gamblerPendingRemoval = $0 },
            onInvite: onInvite
        )
        .padding(.vertical, boxSpacing.medium)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: { withAnimation { isEditing.toggle() } }) {
                    Text(isEditing ? .manageGamblersDoneAction : .manageGamblersEditAction)
                        .fontWeight(isEditing ? .bold : .regular)
                        .foregroundStyle(isEditing ? Color.brandAccent : Color.accentColor)
                }
            }
        }
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
        .overlay(alignment: .bottom) {
            if let failed = viewModel.failedGambler {
                ManageGamblerErrorBanner(
                    username: failed.gamblerUsername,
                    onRetry: { viewModel.remove(failed) }
                )
                .padding(boxSpacing.medium)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.3), value: viewModel.failedGambler?.id)
    }

    private var removalAlertTitle: String {
        guard let member = gamblerPendingRemoval else {
            return ""
        }
        return String(localized: .removeGamblerAlertTitle("@\(member.gamblerUsername)"))
    }
}
