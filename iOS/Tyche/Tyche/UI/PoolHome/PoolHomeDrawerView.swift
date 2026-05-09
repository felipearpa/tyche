import SwiftUI
import UI
import Pool

struct PoolHomeDrawerView: View {
    @ObservedObject var viewModel: PoolHomeDrawerViewModel
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onDeletePool: () -> Void

    init(
        viewModel: PoolHomeDrawerViewModel,
        onLogout: @escaping () -> Void,
        onInvite: @escaping () -> Void,
        onDeletePool: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onSignOut = onLogout
        self.onInvite = onInvite
        self.onDeletePool = onDeletePool
    }

    var body: some View {
        PoolHomeDrawerStatefulView(
            email: viewModel.email,
            poolGamblerScoreState: viewModel.state,
            onSignOut: {
                viewModel.signOut()
                onSignOut()
            },
            onInvite: onInvite,
            onDeletePool: onDeletePool
        )
    }
}

private struct PoolHomeDrawerStatefulView: View {
    let email: String
    let poolGamblerScoreState: LoadableViewState<PoolGamblerScoreModel>
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onDeletePool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            AccountHeader(email: email)
                .frame(maxWidth: .infinity)
                .padding(.top, boxSpacing.large)
                .padding(.horizontal, boxSpacing.medium)
                .padding(.bottom, boxSpacing.medium)

            PoolLayout(poolGamblerScoreState: poolGamblerScoreState)
                .frame(maxWidth: .infinity)

            PoolMenuSection(
                onInvite: onInvite,
                onDeletePool: onDeletePool
            )
            .padding(.horizontal, boxSpacing.medium)
            .padding(.vertical, boxSpacing.large)

            Spacer()

            SignOutButton(onSignOut: onSignOut)
                .frame(maxWidth: .infinity)
                .padding(.all, boxSpacing.medium)
        }
        .frame(maxHeight: .infinity)
    }
}

private struct AccountHeader: View {
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            ZStack {
                Circle()
                    .stroke(Color.accentColor, lineWidth: AVATAR_RING_WIDTH)
                    .frame(width: AVATAR_SIZE, height: AVATAR_SIZE)

                Image(.filledPerson)
                    .resizable()
                    .scaledToFit()
                    .frame(width: AVATAR_ICON_SIZE, height: AVATAR_ICON_SIZE)
                    .foregroundStyle(Color.secondary)
            }

            Text(email)
                .font(.headline)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .truncationMode(.tail)

            Text(String(.connectedAccountText))
                .font(.caption)
                .foregroundStyle(Color.secondary)

            RoundedRectangle(cornerRadius: ACCENT_LINE_HEIGHT / 2)
                .fill(Color.accentColor)
                .frame(width: ACCENT_LINE_WIDTH, height: ACCENT_LINE_HEIGHT)
        }
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
                    icon: { Image(systemName: "user.badge.plus") },
                    title: String(.inviteAction),
                    action: onInvite
                )

                Divider()

                DrawerMenuRow(
                    icon: { Image(sharedResource: .deleteForever) },
                    title: String(.deletePoolAction),
                    tint: .red,
                    action: onDeletePool
                )
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

                Image(sharedResource: .arrowForwardIos)
                    .resizable()
                    .scaledToFit()
                    .frame(width: CHEVRON_SIZE, height: CHEVRON_SIZE)
                    .foregroundStyle(Color.secondary)
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

private let AVATAR_SIZE: CGFloat = 96
private let AVATAR_ICON_SIZE: CGFloat = 56
private let AVATAR_RING_WIDTH: CGFloat = 3
private let ACCENT_LINE_WIDTH: CGFloat = 48
private let ACCENT_LINE_HEIGHT: CGFloat = 3
private let MENU_ICON_SIZE: CGFloat = 22
private let CHEVRON_SIZE: CGFloat = 14
private let SECTION_CORNER_RADIUS: CGFloat = 12

#Preview("Light") {
    PoolHomeDrawerStatefulView(
        email: "felipearpa@email.com",
        poolGamblerScoreState: .success(poolGamblerScoreDummyModel()),
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
        onSignOut: {},
        onInvite: {},
        onDeletePool: {}
    )
}
