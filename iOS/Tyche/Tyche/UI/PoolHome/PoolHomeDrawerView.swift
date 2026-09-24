import SwiftUI
import UI
import Pool
import ViewingState

struct PoolHomeDrawerView: View {
    @ObservedObject var viewModel: PoolHomeDrawerViewModel
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onManageGamblers: () -> Void
    let onPoolDeleting: () -> Void
    let onPoolDeleted: () -> Void
    let onProfile: () -> Void

    @State private var isConfirmingDelete = false

    init(
        viewModel: PoolHomeDrawerViewModel,
        onLogout: @escaping () -> Void,
        onInvite: @escaping () -> Void,
        onManageGamblers: @escaping () -> Void,
        onPoolDeleting: @escaping () -> Void,
        onPoolDeleted: @escaping () -> Void,
        onProfile: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onSignOut = onLogout
        self.onInvite = onInvite
        self.onManageGamblers = onManageGamblers
        self.onPoolDeleting = onPoolDeleting
        self.onPoolDeleted = onPoolDeleted
        self.onProfile = onProfile
    }

    var body: some View {
        PoolHomeDrawerStatefulView(
            uiState: viewModel.uiState,
            onProfile: onProfile,
            onSignOut: onSignOut,
            onInvite: onInvite,
            onManageGamblers: onManageGamblers,
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
    let uiState: PoolHomeDrawerUiState
    let onProfile: () -> Void
    let onSignOut: () -> Void
    let onInvite: () -> Void
    let onManageGamblers: () -> Void
    let onDeletePool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        DrawerMenu(
            accountId: uiState.accountId,
            username: uiState.username,
            email: uiState.email,
            onProfile: onProfile,
            onSignOut: onSignOut
        ) {
            PoolSummary(poolGamblerScoreState: uiState.poolGamblerScoreState)
                .padding(.horizontal, boxSpacing.large)
                .padding(.top, boxSpacing.extraLarge)

            PoolMenuSection(
                isOwner: uiState.isOwner,
                isDeleting: uiState.isDeleting,
                onInvite: onInvite,
                onDeletePool: onDeletePool,
                gamblerCount: uiState.gamblerCount,
                onManageGamblers: onManageGamblers
            )
            .padding(.top, boxSpacing.extraLarge)
        }
    }
}

private struct PoolSummary: View {
    let poolGamblerScoreState: LoadState<PoolGamblerScoreModel>

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        switch poolGamblerScoreState {
        case .idle, .loading:
            PoolSummaryItem(
                poolGamblerScore: poolGamblerScorePlaceholderModel(),
                isPlaceholder: true
            )

        case .loaded(let score):
            PoolSummaryItem(poolGamblerScore: score, isPlaceholder: false)

        case .failure(let error):
            ErrorView(localizedError: error.localizedErrorOrDefault())
                .padding(boxSpacing.large)
                .frame(maxWidth: .infinity)
                .background(
                    Color(sharedResource: .surfaceVariant),
                    in: RoundedRectangle(cornerRadius: SUMMARY_CORNER_RADIUS, style: .continuous)
                )
        }
    }
}

/// The current pool as a restrained, inset group: a small accent detail, the pool name, and the
/// gambler's position and points. Loading renders this same component from the placeholder
/// model under the shared shimmer, hidden from assistive technology.
private struct PoolSummaryItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isPlaceholder: Bool

    @Environment(\.boxSpacing) private var boxSpacing
    @ScaledMetric(relativeTo: .caption) private var trophySize: CGFloat = 14

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.small) {
            HStack(spacing: boxSpacing.small) {
                Image(sharedResource: .trophy)
                    .resizable()
                    .scaledToFit()
                    .frame(width: trophySize, height: trophySize)
                    .foregroundStyle(Color.accentColor)

                Text(.playingNowText)
                    .font(.caption)
                    .foregroundStyle(.drawerSupportingText)
            }

            Text(poolGamblerScore.poolName)
                .font(.headline)
                .foregroundStyle(Color.primary)
                .lineLimit(3)

            PoolStandingText(position: poolGamblerScore.position, score: poolGamblerScore.score)
                .font(.subheadline)
                .foregroundStyle(.drawerSupportingText)
        }
        .modifier(ConditionalShimmer(isActive: isPlaceholder))
        .padding(boxSpacing.large)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            Color(sharedResource: .surfaceVariant),
            in: RoundedRectangle(cornerRadius: SUMMARY_CORNER_RADIUS, style: .continuous)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    private var accessibilityLabel: String {
        [
            String(localized: .playingNowText),
            poolGamblerScore.poolName,
            poolGamblerScore.position.map { String(localized: .poolRankAccessibility($0)) },
            poolGamblerScore.score.map { String(localized: .suffixPointText($0)) },
        ]
        .compactMap { $0 }
        .joined(separator: ", ")
    }
}

/// Position and points on one line, separated by a dot; either can be missing.
private struct PoolStandingText: View {
    let position: Int?
    let score: Int?

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.small) {
            if let position {
                Text(.smallSuffixPosition(position))
            }

            if position != nil, score != nil {
                Text(verbatim: "·")
            }

            if let score {
                Text(.suffixPointText(score))
            }
        }
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
    let gamblerCount: Int?
    let onManageGamblers: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(.poolSectionTitle)
                .font(.footnote.weight(.semibold))
                .textCase(.uppercase)
                .foregroundStyle(.drawerSupportingText)
                .padding(.horizontal, boxSpacing.large)
                .padding(.bottom, boxSpacing.small)
                .accessibilityAddTraits(.isHeader)

            DrawerButtonRow(
                icon: Image(sharedResource: .personAdd),
                title: String(localized: .inviteAction),
                action: onInvite
            )

            if isOwner {
                DrawerButtonRow(
                    icon: Image(sharedResource: .group),
                    title: String(localized: .gamblersAction),
                    accessory: { GamblerCountBadge(gamblerCount: gamblerCount) },
                    action: onManageGamblers
                )

                DrawerButtonRow(
                    icon: Image(sharedResource: .deleteForever),
                    title: String(localized: .deletePoolAction),
                    tint: Color(sharedResource: .error),
                    action: onDeletePool
                )
                .disabled(isDeleting)
            }
        }
    }
}

private struct GamblerCountBadge: View {
    let gamblerCount: Int?

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        if let gamblerCount {
            Text("\(gamblerCount)")
                .font(.footnote)
                .foregroundStyle(Color(sharedResource: .onSurfaceVariant))
                .padding(.horizontal, boxSpacing.small)
                .padding(.vertical, 2)
                .background(Color(sharedResource: .surfaceVariant), in: Capsule())
        }
    }
}

private let SUMMARY_CORNER_RADIUS: CGFloat = 12

private func poolHomeDrawerPreviewUiState(
    poolGamblerScoreState: LoadState<PoolGamblerScoreModel>,
    isOwner: Bool
) -> PoolHomeDrawerUiState {
    PoolHomeDrawerUiState(
        accountId: "account-1",
        email: "felipearpa@email.com",
        username: "felipearpa",
        poolGamblerScoreState: poolGamblerScoreState,
        isOwner: isOwner,
        gamblerCount: 12,
        isDeleting: false
    )
}

#Preview("Light") {
    PoolHomeDrawerStatefulView(
        uiState: poolHomeDrawerPreviewUiState(
            poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
            isOwner: true
        ),
        onProfile: {},
        onSignOut: {},
        onInvite: {},
        onManageGamblers: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    PoolHomeDrawerStatefulView(
        uiState: poolHomeDrawerPreviewUiState(
            poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
            isOwner: true
        ),
        onProfile: {},
        onSignOut: {},
        onInvite: {},
        onManageGamblers: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Without position") {
    PoolHomeDrawerStatefulView(
        uiState: poolHomeDrawerPreviewUiState(
            poolGamblerScoreState: .loaded(poolGamblerScoreWithoutPositionDummyModel()),
            isOwner: true
        ),
        onProfile: {},
        onSignOut: {},
        onInvite: {},
        onManageGamblers: {},
        onDeletePool: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Loading") {
    PoolHomeDrawerStatefulView(
        uiState: poolHomeDrawerPreviewUiState(
            poolGamblerScoreState: .loading,
            isOwner: false
        ),
        onProfile: {},
        onSignOut: {},
        onInvite: {},
        onManageGamblers: {},
        onDeletePool: {}
    )
}

#Preview("Failure") {
    PoolHomeDrawerStatefulView(
        uiState: poolHomeDrawerPreviewUiState(
            poolGamblerScoreState: .failure(UnknownLocalizedError()),
            isOwner: true
        ),
        onProfile: {},
        onSignOut: {},
        onInvite: {},
        onManageGamblers: {},
        onDeletePool: {}
    )
}

#Preview("Largest text in drawer") {
    Color.clear
        .drawer(isShowing: .constant(true)) {
            PoolHomeDrawerStatefulView(
                uiState: poolHomeDrawerPreviewUiState(
                    poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
                    isOwner: true
                ),
                onProfile: {},
                onSignOut: {},
                onInvite: {},
                onManageGamblers: {},
                onDeletePool: {}
            )
        }
        .dynamicTypeSize(.accessibility5)
}

#Preview("Right-to-left in drawer") {
    Color.clear
        .drawer(isShowing: .constant(true)) {
            PoolHomeDrawerStatefulView(
                uiState: poolHomeDrawerPreviewUiState(
                    poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
                    isOwner: true
                ),
                onProfile: {},
                onSignOut: {},
                onInvite: {},
                onManageGamblers: {},
                onDeletePool: {}
            )
        }
        .environment(\.layoutDirection, .rightToLeft)
}

#Preview("Short window") {
    Color.clear
        .drawer(isShowing: .constant(true)) {
            PoolHomeDrawerStatefulView(
                uiState: poolHomeDrawerPreviewUiState(
                    poolGamblerScoreState: .loaded(poolGamblerScoreDummyModel()),
                    isOwner: true
                ),
                onProfile: {},
                onSignOut: {},
                onInvite: {},
                onManageGamblers: {},
                onDeletePool: {}
            )
        }
        .frame(width: 874, height: 360)
}
