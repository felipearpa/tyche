import SwiftUI
import UI

struct PoolScoreListDrawerView : View {
    @StateObject var viewModel: PoolScoreListDrawerViewModel
    let onSignOut: () -> Void
    let onProfile: () -> Void

    init(
        viewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        onSignOut: @escaping () -> Void,
        onProfile: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onSignOut = onSignOut
        self.onProfile = onProfile
    }

    var body: some View {
        PoolScoreListDrawerStatefulView(
            uiState: viewModel.uiState,
            onProfile: onProfile,
            onSignOut: {
                viewModel.logOut()
                onSignOut()
            }
        )
    }
}

private struct PoolScoreListDrawerStatefulView : View {
    let uiState: PoolScoreListDrawerUiState
    let onProfile: () -> Void
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            AccountHeaderDrawer(
                accountId: uiState.accountId,
                username: uiState.username,
                email: uiState.email,
                onProfile: onProfile
            )
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, boxSpacing.large)
                .padding(.horizontal, boxSpacing.medium)

            Spacer()

            SignOutButton(onSignOut: onSignOut)
                .padding(boxSpacing.medium)
        }
    }
}

private struct SignOutButton: View {
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        Button(action: onSignOut) {
            HStack(spacing: boxSpacing.small) {
                Image(.logOut)
                    .renderingMode(.template)
                Text(.signOutAction)
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
        .buttonStyle(.liquidGlass)
    }
}

#Preview {
    PoolScoreListDrawerStatefulView(
        uiState: PoolScoreListDrawerUiState(
            accountId: "account-1",
            email: "felipearpa@email.com",
            username: "felipearpa"
        ),
        onProfile: {},
        onSignOut: {}
    )
}
