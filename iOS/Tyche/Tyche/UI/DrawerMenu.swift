import SwiftUI
import UI

/// The hierarchy both drawers share: account identity with Profile directly below it, any
/// sections, then a separated sign-out footer. The drawer container gives the menu at least
/// the window's height, so the footer rests at the bottom when there is room and scrolls with
/// the menu when large text or a short window leaves none.
struct DrawerMenu<Sections: View>: View {
    let accountId: String
    let username: String
    let email: String
    let onProfile: () -> Void
    let onSignOut: () -> Void
    let sections: Sections

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        accountId: String,
        username: String,
        email: String,
        onProfile: @escaping () -> Void,
        onSignOut: @escaping () -> Void,
        @ViewBuilder sections: () -> Sections
    ) {
        self.accountId = accountId
        self.username = username
        self.email = email
        self.onProfile = onProfile
        self.onSignOut = onSignOut
        self.sections = sections()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AccountHeaderDrawer(
                accountId: accountId,
                username: username,
                email: email,
                onProfile: onProfile
            )
            .padding(.top, boxSpacing.large)

            sections

            Spacer(minLength: boxSpacing.extraLarge)

            SignOutFooter(onSignOut: onSignOut)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension DrawerMenu where Sections == EmptyView {
    init(
        accountId: String,
        username: String,
        email: String,
        onProfile: @escaping () -> Void,
        onSignOut: @escaping () -> Void
    ) {
        self.init(
            accountId: accountId,
            username: username,
            email: email,
            onProfile: onProfile,
            onSignOut: onSignOut,
            sections: { EmptyView() }
        )
    }
}

private struct SignOutFooter: View {
    let onSignOut: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            DrawerButtonRow(
                icon: Image(.logOut),
                title: String(localized: .signOutAction),
                action: onSignOut
            )
            .padding(.vertical, boxSpacing.medium)
        }
    }
}

extension ShapeStyle where Self == Color {
    /// Supporting drawer text such as the email, section titles, and pool statistics. It stays
    /// subdued on the drawer and pool-summary surfaces while keeping at least 4.5:1 contrast,
    /// which the system secondary label (about 3.4:1 on light surfaces) does not reach.
    static var drawerSupportingText: Color {
        Color(sharedResource: .onSurface).opacity(supportingTextOpacity)
    }
}

private let supportingTextOpacity: CGFloat = 0.7
