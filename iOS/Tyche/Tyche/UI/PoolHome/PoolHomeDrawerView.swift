import SwiftUI
import UI
import Pool

struct PoolHomeDrawerView: View {
    @ObservedObject var viewModel: PoolHomeDrawerViewModel
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onPoolDeleting: () -> Void
    let onPoolDeleted: () -> Void

    @State private var isConfirmingDelete = false

    init(
        viewModel: PoolHomeDrawerViewModel,
        onLogout: @escaping () -> Void,
        onInvite: @escaping () -> Void,
        onPoolDeleting: @escaping () -> Void,
        onPoolDeleted: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onSignOut = onLogout
        self.onInvite = onInvite
        self.onPoolDeleting = onPoolDeleting
        self.onPoolDeleted = onPoolDeleted
    }

    var body: some View {
        PoolHomeDrawerStatefulView(
            email: viewModel.email,
            poolGamblerScoreState: viewModel.state,
            isOwner: viewModel.isOwner,
            isDeleting: viewModel.deleteState.isLoading(),
            onSignOut: {
                viewModel.signOut()
                onSignOut()
            },
            onInvite: onInvite,
            onDeletePool: { isConfirmingDelete = true }
        )
        .alert(
            String(.deletePoolAlertTitle),
            isPresented: $isConfirmingDelete
        ) {
            Button(String(.cancelAction), role: .cancel) {}
            Button(String(.deleteAction), role: .destructive) {
                onPoolDeleting()
                viewModel.deletePool(onSuccess: onPoolDeleted)
            }
        } message: {
            Text(String(.deletePoolAlertMessage))
        }
    }
}

private struct PoolHomeDrawerStatefulView: View {
    let email: String
    let poolGamblerScoreState: LoadableViewState<PoolGamblerScoreModel>
    let isOwner: Bool
    let isDeleting: Bool
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onDeletePool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            AccountHeaderDrawer(email: email)
                .frame(maxWidth: .infinity)
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
    let poolGamblerScoreState: LoadableViewState<PoolGamblerScoreModel>

    var body: some View {
        switch poolGamblerScoreState {
        case .initial, .loading:
            PoolLayoutItem(
                poolGamblerScore: poolGamblerScorePlaceholderModel(),
                isPlaceholder: true
            )

        case .success(let score):
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

                Text(String(.playingNowText))
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

                    Text(String(format: String(.suffixPointText), score))
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
            Text(String(.poolSectionTitle).uppercased())
                .font(.caption)
                .foregroundStyle(Color.secondary)

            VStack(spacing: 0) {
                DrawerMenuRow(
                    icon: { Image(sharedResource: .personAdd) },
                    title: String(.inviteAction),
                    action: onInvite
                )

                if isOwner {
                    Divider()

                    DrawerMenuRow(
                        icon: { Image(sharedResource: .deleteForever) },
                        title: String(.deletePoolAction),
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
                Text(String(.logOutAction))
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
        poolGamblerScoreState: .success(poolGamblerScoreDummyModel()),
        isOwner: true,
        isDeleting: false,
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    PoolHomeDrawerStatefulView(
        email: "felipearpa@email.com",
        poolGamblerScoreState: .success(poolGamblerScoreDummyModel()),
        isOwner: true,
        isDeleting: false,
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Loading") {
    PoolHomeDrawerStatefulView(
        email: "felipearpa@email.com",
        poolGamblerScoreState: .loading,
        isOwner: false,
        isDeleting: false,
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
}
