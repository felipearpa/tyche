import SwiftUI
import UI

struct AccountHeaderDrawer: View {
    let username: String
    let email: String
    let onEditAccount: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
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

            Button(action: onEditAccount) {
                HStack(spacing: boxSpacing.large) {
                    Image(sharedResource: .edit)
                    Text(.editProfileAction)
                    Spacer()
                }
            }
            .buttonStyle(.plain)
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
