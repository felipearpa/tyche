import SwiftUI
import UI

struct PoolScoreListDrawerView : View {
    @StateObject var viewModel: PoolScoreListDrawerViewModel
    let onSignOut: () -> Void
    let onEditAccount: () -> Void

    init(
        viewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        onSignOut: @escaping () -> Void,
        onEditAccount: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onSignOut = onSignOut
        self.onEditAccount = onEditAccount
    }

    var body: some View {
        PoolScoreListDrawerStatefulView(
            email: viewModel.email,
            username: viewModel.username,
            onEditAccount: onEditAccount,
            onSignOut: {
                viewModel.logOut()
                onSignOut()
            }
        )
    }
}

private struct PoolScoreListDrawerStatefulView : View {
    let email: String
    let username: String
    let onEditAccount: () -> Void
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            AccountHeaderDrawer(
                username: username,
                email: email,
                onEditAccount: onEditAccount
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
                Text(.logOutAction)
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
        .buttonStyle(.liquidGlass)
    }
}

#Preview {
    PoolScoreListDrawerStatefulView(
        email: "felipearpa@email.com",
        username: "felipearpa",
        onEditAccount: {},
        onSignOut: {}
    )
}
