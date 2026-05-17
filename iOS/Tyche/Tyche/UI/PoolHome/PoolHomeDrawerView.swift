import SwiftUI
import UI
import Pool
import ViewingState

struct PoolHomeDrawerView: View {
    @ObservedObject var viewModel: PoolHomeDrawerViewModel
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onPoolDeleting: () -> Void
    let onPoolDeleted: () -> Void
    let onEditAccount: () -> Void

    @State private var isConfirmingDelete = false

    init(
        viewModel: PoolHomeDrawerViewModel,
        onLogout: @escaping () -> Void,
        onInvite: @escaping () -> Void,
        onPoolDeleting: @escaping () -> Void,
        onPoolDeleted: @escaping () -> Void,
        onEditAccount: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onSignOut = onLogout
        self.onInvite = onInvite
        self.onPoolDeleting = onPoolDeleting
        self.onPoolDeleted = onPoolDeleted
        self.onEditAccount = onEditAccount
    }

    var body: some View {
        PoolHomeDrawerStatefulView(
            email: viewModel.email,
            username: viewModel.username,
            poolGamblerScoreState: viewModel.state,
            isOwner: viewModel.isOwner,
            isDeleting: viewModel.deleteState.isLoading(),
            onEditAccount: onEditAccount,
            onSignOut: {
                viewModel.signOut()
                onSignOut()
            },
            onInvite: onInvite,
            onDeletePool: { isConfirmingDelete = true }
        )
        .alert(
            String(localized: .deletePoolAlertTitle),
            isPresented: $isConfirmingDelete
        ) {
            Button(String(localized: .cancelAction), role: .cancel) {}
            Button(String(localized: .deleteAction), role: .destructive) {
                onPoolDeleting()
                viewModel.deletePool(onSuccess: onPoolDeleted)
            }
        } message: {
            Text(.deletePoolAlertMessage)
        }
    }
}

private struct PoolHomeDrawerStatefulView: View {
    let email: String
    let username: String
    let poolGamblerScoreState: LoadState<PoolGamblerScoreModel>
    let isOwner: Bool
    let isDeleting: Bool
    let onEditAccount: () -> Void
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onDeletePool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            AccountHeaderDrawer(
                username: username,
                email: email,
                onEditAccount: onEditAccount
            )
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, boxSpacing.large)
                .padding(.horizontal, boxSpacing.medium)
                .padding(.bottom, boxSpacing.medium)

            PoolLayout(poolGamblerScoreState: poolGamblerScoreState)
                .frame(maxWidth: .infinity)

            PoolMenuSection(
                isOwner: isOwner,
                isDeleting: isDeleting,
                onInvite: onInvite,
                onDeletePool: onDeletePool
            )
            .padding(.horizontal, boxSpacing.medium)
            .padding(.top, boxSpacing.medium)

            Spacer()

            SignOutButton(onSignOut: onSignOut)
                .frame(maxWidth: .infinity)
                .padding(.all, boxSpacing.medium)
        }
        .frame(maxHeight: .infinity)
    }
}

private struct PoolLayout: View {
    let poolGamblerScoreState: LoadState<PoolGamblerScoreModel>

    var body: some View {
        switch poolGamblerScoreState {
        case .idle, .loading:
            PoolLayoutItem(
                poolGamblerScore: poolGamblerScorePlaceholderModel(),
                isPlaceholder: true
            )

        case .loaded(let score):
            PoolLayoutItem(poolGamblerScore: score, isPlaceholder: false)

        case .failure(let error):
            ErrorView(localizedError: error.localizedErrorOrDefault())
        }
    }
}

private struct PoolLayoutItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isPlaceholder: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.small) {
            HStack(spacing: boxSpacing.small) {
                Image(sharedResource: .trophy)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .foregroundStyle(Color.white)
                    .modifier(ConditionalShimmer(isActive: isPlaceholder))

                Text(.playingNowText)
                    .font(.caption)
                    .foregroundStyle(Color.white)
                    .modifier(ConditionalShimmer(isActive: isPlaceholder))
            }

            Text(poolGamblerScore.poolName)
                .font(.title2)
                .foregroundStyle(Color.white)
                .lineLimit(2)
                .truncationMode(.tail)
                .modifier(ConditionalShimmer(isActive: isPlaceholder))

            HStack(spacing: boxSpacing.medium) {
                Text("\(poolGamblerScore.position ?? 0)º")
                    .font(.subheadline)
                    .foregroundStyle(Color.white)
                    .modifier(ConditionalShimmer(isActive: isPlaceholder))

                if let score = poolGamblerScore.score {
                    Rectangle()
                        .fill(Color.white)
                        .frame(width: 1)
                        .frame(maxHeight: .infinity)

                    Text(.suffixPointText(score))
                        .font(.subheadline)
                        .foregroundStyle(Color.white)
                        .modifier(ConditionalShimmer(isActive: isPlaceholder))
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.all, boxSpacing.large)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.accentColor)
    }
}

private struct ConditionalShimmer: ViewModifier {
    let isActive: Bool

    func body(content: Content) -> some View {
        if isActive {
            content.shimmer()
        } else {
            content
        }
    }
}

private struct PoolMenuSection: View {
    let isOwner: Bool
    let isDeleting: Bool
    let onInvite: () -> Void
    let onDeletePool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.small) {
            Text(String(localized: .poolSectionTitle).uppercased())
                .font(.caption)
                .foregroundStyle(Color.secondary)

            VStack(spacing: 0) {
                DrawerMenuRow(
                    icon: { Image(sharedResource: .personAdd) },
                    title: String(localized: .inviteAction),
                    action: onInvite
                )

                if isOwner {
                    Divider()

                    DrawerMenuRow(
                        icon: { Image(sharedResource: .deleteForever) },
                        title: String(localized: .deletePoolAction),
                        tint: Color(sharedResource: .error),
                        action: onDeletePool
                    )
                    .disabled(isDeleting)
                }
            }
            .padding(.horizontal, boxSpacing.medium)
            .overlay(
                RoundedRectangle(cornerRadius: SECTION_CORNER_RADIUS)
                    .stroke(Color(.separator), lineWidth: 1)
            )
        }
    }
}

private struct DrawerMenuRow<Icon: View>: View {
    let icon: Icon
    let title: String
    var tint: Color = .primary
    let action: () -> Void

    init(
        @ViewBuilder icon: () -> Icon,
        title: String,
        tint: Color = .primary,
        action: @escaping () -> Void
    ) {
        self.icon = icon()
        self.title = title
        self.tint = tint
        self.action = action
    }

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        Button(action: action) {
            HStack(spacing: boxSpacing.medium) {
                icon
                    .frame(width: MENU_ICON_SIZE, height: MENU_ICON_SIZE)
                    .foregroundStyle(tint)

                Text(title)
                    .foregroundStyle(tint)

                Spacer()
            }
            .padding(.vertical, boxSpacing.medium)
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
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                Text(.logOutAction)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.liquidGlass)
    }
}

private let MENU_ICON_SIZE: CGFloat = 22
private let CHEVRON_SIZE: CGFloat = 14
private let SECTION_CORNER_RADIUS: CGFloat = 12

#Preview("Light") {
    PoolHomeDrawerStatefulView(
        email: "felipearpa@email.com",
        username: "felipearpa",
        poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
        isOwner: true,
        isDeleting: false,
        onEditAccount: {},
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    PoolHomeDrawerStatefulView(
        email: "felipearpa@email.com",
        username: "felipearpa",
        poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
        isOwner: true,
        isDeleting: false,
        onEditAccount: {},
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Loading") {
    PoolHomeDrawerStatefulView(
        email: "felipearpa@email.com",
        username: "felipearpa",
        poolGamblerScoreState: .loading,
        isOwner: false,
        isDeleting: false,
        onEditAccount: {},
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
}
