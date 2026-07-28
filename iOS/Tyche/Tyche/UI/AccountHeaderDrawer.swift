import SwiftUI
import UI
import Account

struct AccountHeaderDrawer: View {
    let accountId: String
    let username: String
    let email: String
    let onProfile: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            AccountIdentityRow(
                accountId: accountId,
                username: username,
                email: email
            )

            DrawerButtonRow(icon: {
                Image(systemName: "person.crop.circle")
                    .resizable()
                    .scaledToFit()
            }, title: String(localized: .profileTitle)) {
                onProfile()
            }
        }
    }
}

private struct AccountIdentityRow: View {
    let accountId: String
    let username: String
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            AccountAvatar(accountId: accountId, email: email)
                .frame(width: AVATAR_SIZE, height: AVATAR_SIZE)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: boxSpacing.small) {
                Text(username.isEmpty ? email : username)
                    .font(.headline)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Text(email)
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private let AVATAR_SIZE: CGFloat = 48

#Preview {
    AccountHeaderDrawer(
        accountId: "account-1",
        username: "felipearpa",
        email: "felipearpa@tyche.com",
        onProfile: {}
    )
}
