import SwiftUI
import UI
import Account

struct AccountHeaderDrawer: View {
    let username: String
    let email: String
    let onEditAccount: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: boxSpacing.medium) {
                EmailAvatar(email: email)
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

            DrawerButtonRow(icon: {
                Image(sharedResource: .edit)
            }, title: String(localized: .editUsernameAction)) {
                onEditAccount()
            }
        }
    }
}

private let AVATAR_SIZE: CGFloat = 48

#Preview {
    AccountHeaderDrawer(
        username: "felipearpa",
        email: "felipearpa@tyche.com",
        onEditAccount: {}
    )
}
