import SwiftUI

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
            AccountHeader(email: email)
                .padding(.top, boxSpacing.large)
                .padding(.horizontal, boxSpacing.medium)

            Spacer()

            SignOutButton(onSignOut: onSignOut)
                .padding(boxSpacing.medium)
        }
    }
}

private struct AccountHeader: View {
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            ZStack {
                Circle()
                    .stroke(Color.accentColor, lineWidth: avatarRingWidth)
                    .frame(width: avatarSize, height: avatarSize)

                Image(.filledPerson)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: avatarIconSize, height: avatarIconSize)
                    .foregroundStyle(.secondary)
            }

            VStack(spacing: boxSpacing.small) {
                Text(email)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)

                Text(String(.connectedAccountText))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Rectangle()
                .fill(Color.accentColor)
                .frame(width: accentLineWidth, height: accentLineHeight)
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity)
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
                Text(String(.logOutAction))
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
        .buttonStyle(.bordered)
    }
}

private let avatarSize: CGFloat = 96
private let avatarIconSize: CGFloat = 56
private let avatarRingWidth: CGFloat = 3
private let accentLineWidth: CGFloat = 48
private let accentLineHeight: CGFloat = 3

#Preview {
    PoolScoreListDrawerStatefulView(email: "felipearpa@email.com", onSignOut: {})
}
