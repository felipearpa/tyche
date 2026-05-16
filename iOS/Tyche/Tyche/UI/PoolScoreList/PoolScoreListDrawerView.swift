import SwiftUI
import UI

struct PoolScoreListDrawerView : View {
    @StateObject var viewModel: PoolScoreListDrawerViewModel
    let email: String
    let onSignOut: () -> Void

    init(
        viewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        email: String,
        onSignOut: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.email = email
        self.onSignOut = onSignOut
    }

    var body: some View {
        PoolScoreListDrawerStatefulView(email: email, onSignOut: {
            viewModel.logOut()
            onSignOut()
        })
    }
}

private struct PoolScoreListDrawerStatefulView : View {
    let email: String
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            AccountHeaderDrawer(email: email)
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
    PoolScoreListDrawerStatefulView(email: "felipearpa@email.com", onSignOut: {})
}
