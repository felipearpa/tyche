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
        VStack(alignment: .leading, spacing: boxSpacing.medium) {
            AccountIdentityRow(
                accountId: accountId,
                username: username,
                email: email
            )
            .padding(.horizontal, boxSpacing.large)
            .drawerInitialFocus()

            DrawerButtonRow(
                icon: Image(sharedResource: .filledPerson),
                title: String(localized: .profileTitle),
                action: onProfile
            )
        }
    }
}

private struct AccountIdentityRow: View {
    let accountId: String
    let username: String
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        // At accessibility sizes the name and email get the full drawer width below the avatar
        // instead of wrapping into a narrow column beside it.
        let layout = dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: boxSpacing.medium))
            : AnyLayout(HStackLayout(spacing: boxSpacing.medium))

        layout {
            AccountAvatar(accountId: accountId, email: email)
                .frame(width: drawerLeadingColumnWidth, height: drawerLeadingColumnWidth)
                .clipShape(Circle())
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: boxSpacing.small) {
                Text(username.isEmpty ? email : username)
                    .font(.headline)
                    .foregroundStyle(Color.primary)
                    .lineLimit(2)

                Text(email)
                    .font(.caption)
                    .foregroundStyle(.drawerSupportingText)
                    .lineLimit(2)
                    .truncationMode(.middle)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview {
    AccountHeaderDrawer(
        accountId: "account-1",
        username: "felipearpa",
        email: "felipearpa@tyche.com",
        onProfile: {}
    )
}
