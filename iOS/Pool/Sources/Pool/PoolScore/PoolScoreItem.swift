import SwiftUI
import UI

struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onOpen: () -> Void
    let onJoin: () -> Void
    let placeholderModifier: (any ViewModifier)?

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// The rank tile and invite icon grow with Dynamic Type. Unlike the leaderboard's rank
    /// rail, this row's rail has no fixed width, so the tile can afford to scale.
    @ScaledMetric(relativeTo: .body) private var rankTileSize: CGFloat = 32
    @ScaledMetric(relativeTo: .body) private var inviteIconSize: CGFloat = 20

    init(
        poolGamblerScore: PoolGamblerScoreModel,
        onOpen: @escaping () -> Void,
        onJoin: @escaping () -> Void,
        placeholderModifier: (any ViewModifier)? = nil
    ) {
        self.poolGamblerScore = poolGamblerScore
        self.onOpen = onOpen
        self.onJoin = onJoin
        self.placeholderModifier = placeholderModifier
    }

    var isPlaceholder: Bool {
        placeholderModifier != nil
    }

    var body: some View {
        Group {
            if let placeholderModifier {
                scoreContent(applying: placeholderModifier)
            } else {
                scoreContent
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(.isButton)
        .accessibilityHint(Text(.poolScoreOpenAccessibilityAction))
        .accessibilityAction {
            onOpen()
        }
        .accessibilityAction(named: Text(.poolScoreInviteAccessibilityAction)) {
            onJoin()
        }
        .accessibilityHidden(isPlaceholder)
    }

    private func scoreContent(applying modifier: some ViewModifier) -> AnyView {
        AnyView(scoreContent.modifier(modifier))
    }

    private var scoreContent: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                reflowedContent
            } else {
                inlineContent
            }
        }
        .padding(boxSpacing.large)
        .frame(maxWidth: .infinity)
    }

    /// The normal row: rank rail, identity, then the invite control on the trailing edge.
    private var inlineContent: some View {
        HStack(spacing: boxSpacing.medium) {
            rankRail

            VStack(alignment: .leading) {
                Text(poolGamblerScore.poolName)

                metaCounts
            }

            Spacer()

            inviteButton
        }
    }

    /// At accessibility text sizes three columns cannot share 400 points: the identity column
    /// got so narrow that the pool name and member count broke mid-word ("Mund / ial",
    /// "me / mbe / rs"). The row reflows to full-width rows instead, so every string gets the
    /// whole width and wraps on word boundaries.
    private var reflowedContent: some View {
        VStack(alignment: .leading, spacing: boxSpacing.medium) {
            HStack(spacing: boxSpacing.medium) {
                rankRail

                Spacer()

                inviteButton
            }

            Text(poolGamblerScore.poolName)

            metaCounts
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var rankRail: some View {
        VStack(spacing: boxSpacing.small) {
            if let position = poolGamblerScore.position {
                PostionIndicator(
                    position: position,
                    shouldUsePrimeryColor: false,
                    size: rankTileSize
                )
            }

            if let rank = poolGamblerScore.rank() {
                TrendIndicator(difference: rank, textStyle: Font.TextStyle.footnote)
            }
        }
    }

    private var inviteButton: some View {
        Button(action: onJoin) {
            Image(.personAdd)
                .resizable()
                .scaledToFit()
                .frame(width: inviteIconSize, height: inviteIconSize)
        }
        .buttonStyle(.liquidGlass)
        .buttonBorderShape(.capsule)
        .accessibilityLabel(Text(.poolScoreInviteAccessibilityAction))
    }

    /// Points and member count. They sit side by side normally, but at accessibility text
    /// sizes there is no room for two columns — squeezed into one they wrapped mid-word
    /// ("me / mbe / rs"), so they stack instead and each keeps a single, shrink-to-fit line.
    @ViewBuilder
    private var metaCounts: some View {
        let score = poolGamblerScore.score.map { Text(.pointsText($0)).font(.footnote.bold()) }
        let members = poolGamblerScore.gamblerCount.map { Text(.gamblersText($0)).font(.footnote) }

        if dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: boxSpacing.small) {
                score
                members
            }
        } else {
            HStack(spacing: boxSpacing.large) {
                score
                members
            }
            .lineLimit(1)
            .minimumScaleFactor(countMinimumScaleFactor)
        }
    }

    /// The exact string handed to `.accessibilityLabel` in `body`. A placeholder row
    /// contributes no VoiceOver text, so it resolves to the empty string.
    ///
    /// Unlike the leaderboard row, this one leads with the pool name: these rows are
    /// not a ranking, so the name is what distinguishes one row from the next.
    var accessibilityLabel: String {
        isPlaceholder ? "" : loadedAccessibilityLabel
    }

    private var loadedAccessibilityLabel: String {
        [
            poolGamblerScore.poolName,
            rankAccessibilityText,
            scoreAccessibilityText,
            gamblerCountAccessibilityText,
            movementAccessibilityText,
        ]
        .compactMap { $0 }
        .joined(separator: ", ")
    }

    private var rankAccessibilityText: String {
        guard let position = poolGamblerScore.position else {
            return String(localized: .leaderboardRankMissingAccessibility)
        }
        return String(localized: .leaderboardRankAccessibility(position))
    }

    private var scoreAccessibilityText: String {
        guard let score = poolGamblerScore.score else {
            return String(localized: .leaderboardScoreMissingAccessibility)
        }
        return String(localized: .leaderboardPointsAccessibility(score))
    }

    /// The visible member count already spells the word out, so the announcement
    /// reuses its string rather than duplicating it under an accessibility key —
    /// including its plural agreement, since the row below renders the same symbol.
    private var gamblerCountAccessibilityText: String? {
        guard let gamblerCount = poolGamblerScore.gamblerCount else { return nil }
        return String(localized: .gamblersText(gamblerCount))
    }

    private var movementAccessibilityText: String? {
        guard let difference = poolGamblerScore.rank() else { return nil }

        switch difference {
        case let value where value > 0:
            return String(localized: .leaderboardMovementUpAccessibility(abs(value)))
        case let value where value < 0:
            return String(localized: .leaderboardMovementDownAccessibility(abs(value)))
        default:
            return String(localized: .leaderboardMovementUnchangedAccessibility)
        }
    }
}

private let countMinimumScaleFactor: CGFloat = 0.7

#Preview("Normal Light") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onOpen: {}, onJoin: {})
        .preferredColorScheme(.light)
}

#Preview("Normal Dark") {
    PoolScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), onOpen: {}, onJoin: {})
        .preferredColorScheme(.dark)
}

#Preview("Without position") {
    PoolScoreItem(
        poolGamblerScore: poolGamblerScoreWithoutPositionDummyModel(),
        onOpen: {},
        onJoin: {}
    )
}

#Preview("Long pool name") {
    PoolScoreItem(
        poolGamblerScore: PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "This is a very long pool name to test how it is displayed in the list",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            position: 4,
            beforePosition: 3,
            score: 8,
            gamblerCount: 101
        ),
        onOpen: {},
        onJoin: {}
    )
}

#Preview("Placeholder") {
    PoolScoreItem(
        poolGamblerScore: poolGamblerScorePlaceholderModel(),
        onOpen: {},
        onJoin: {},
        placeholderModifier: ShimmerModifier()
    )
}
