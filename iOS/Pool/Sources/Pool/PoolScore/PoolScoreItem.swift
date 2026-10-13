import Core
import SwiftUI
import UI

/// One "My pools" row. Loading rows render this same component from
/// `poolGamblerScorePlaceholderModel()` with `isPlaceholder: true`; the row then conceals its
/// content with native redaction and the shared `LoadingPlaceholderPulse`, ignores touches,
/// offers no open or invite action, and is hidden from assistive technology.
struct PoolScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let onOpen: () -> Void
    let onJoin: () -> Void
    let isPlaceholder: Bool

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// The rank tile and invite icon grow with Dynamic Type. Unlike the leaderboard's rank
    /// rail, this row's rail has no fixed width, so the tile can afford to scale.
    @ScaledMetric(relativeTo: .body) private var rankTileSize: CGFloat = 32
    @ScaledMetric(relativeTo: .body) private var inviteIconSize: CGFloat = 20
    @ScaledMetric(relativeTo: .footnote) private var rankRailMaximumWidth: CGFloat = 72

    init(
        poolGamblerScore: PoolGamblerScoreModel,
        onOpen: @escaping () -> Void,
        onJoin: @escaping () -> Void,
        isPlaceholder: Bool = false
    ) {
        self.poolGamblerScore = poolGamblerScore
        self.onOpen = onOpen
        self.onJoin = onJoin
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { scoreContent }
            } else {
                scoreContent
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
            }
        }
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
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

            // Wraps to as many lines as it needs; a taller rank rail must not squeeze it.
            Text(poolGamblerScore.poolName)
                .fixedSize(horizontal: false, vertical: true)

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
                    size: rankTileSize,
                    isPlaceholder: isPlaceholder
                )
            }

            TrendIndicator(
                movement: poolGamblerScore.movement,
                textStyle: Font.TextStyle.footnote,
                isPlaceholder: isPlaceholder
            )
        }
        // In the inline row a partial group's qualifier or a pending or unavailable label wraps on
        // word boundaries within a capped rail; a definite width lets it report its full height.
        // The reflowed row gives the rail its own line, so the label keeps its natural width there.
        .frame(width: labeledRailWidth)
    }

    private var labeledRailWidth: CGFloat? {
        guard poolGamblerScore.movement.hasLabel, !dynamicTypeSize.isAccessibilitySize else { return nil }
        return rankRailMaximumWidth
    }

    private var inviteButton: some View {
        Button(action: onJoin) {
            Image(.personAdd)
                .resizable()
                .scaledToFit()
                .frame(width: inviteIconSize, height: inviteIconSize)
        }
        .buttonStyle(.bordered)
        .buttonBorderShape(.capsule)
        // The accent glyph measures 2.3:1 on the bordered fill in light appearance, below the 3:1
        // needed to identify the control; the primary color keeps the glyph as legible as it was
        // on the glass control.
        .tint(.primary)
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

    private var movementAccessibilityText: String {
        poolGamblerScore.movement.accessibilityText
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
            gamblerCount: 101,
            trendGroup: KickoffGroupProgress(computedMatches: 2, totalMatches: 4)
        ),
        onOpen: {},
        onJoin: {}
    )
}

#Preview("Trend states") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(poolGamblerScoreTrendStateDummyModels()) { score in
                PoolScoreItem(poolGamblerScore: score, onOpen: {}, onJoin: {})
                Divider()
            }
        }
    }
}

#Preview("Trend states, accessibility text") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(poolGamblerScoreTrendStateDummyModels()) { score in
                PoolScoreItem(poolGamblerScore: score, onOpen: {}, onJoin: {})
                Divider()
            }
        }
    }
    .environment(\.dynamicTypeSize, .accessibility3)
}

#Preview("Placeholder") {
    PoolScoreItem(
        poolGamblerScore: poolGamblerScorePlaceholderModel(),
        onOpen: {},
        onJoin: {},
        isPlaceholder: true
    )
}
