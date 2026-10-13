import Account
import Core
import Foundation
import SwiftUI
import UI

/// One leaderboard row. Loading rows render this same component from
/// `poolGamblerScorePlaceholderModel()` with `isPlaceholder: true`; the row then conceals its
/// content with native redaction and the shared `LoadingPlaceholderPulse`, ignores touches,
/// requests no avatar, and is hidden from assistive technology. Callers pass no effect.
public struct GamblerScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isCurrentUser: Bool
    let isPlaceholder: Bool

    /// Room for a partial group's qualifier or a pending or unavailable label beneath the tile;
    /// it grows with the qualifier's text size.
    @ScaledMetric(relativeTo: .caption2) private var rankRailMaximumWidth: CGFloat = 64
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    public init(
        poolGamblerScore: PoolGamblerScoreModel,
        isCurrentUser: Bool,
        isPlaceholder: Bool = false
    ) {
        self.poolGamblerScore = poolGamblerScore
        self.isCurrentUser = isCurrentUser
        self.isPlaceholder = isPlaceholder
    }

    public var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { scoreContent }
            } else {
                scoreContent
            }
        }
        .frame(maxWidth: .infinity, minHeight: rowMinimumHeight)
        .background(rowBackground)
        .allowsHitTesting(!isPlaceholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    @ViewBuilder
    private var scoreContent: some View {
        if dynamicTypeSize.isAccessibilitySize {
            reflowedContent
        } else {
            inlineContent
        }
    }

    /// The ordinary row: rank rail, avatar, a one-line username ellipsized before the score, and
    /// the trailing score.
    private var inlineContent: some View {
        HStack(spacing: identitySpacing) {
            rankRail
            avatar

            VStack(alignment: .leading, spacing: identityTextSpacing) {
                Text(poolGamblerScore.gamblerUsername)
                    .font(.body.weight(.semibold))
                    .lineLimit(1)
                    .truncationMode(.tail)

                youMarker
            }
            .loadedForeground(rowForeground, isPlaceholder: isPlaceholder)
            .frame(maxWidth: .infinity, alignment: .leading)
            .layoutPriority(1)

            score
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .frame(minWidth: scoreMinimumWidth, alignment: .trailing)
        }
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, verticalPadding)
    }

    /// At accessibility text sizes one line cannot hold the rail, avatar, username, and score
    /// without clipping or truncating them. The rail, avatar, and score share the first line
    /// (the score moves below when even that does not fit), and the username wraps across the
    /// full width beneath them.
    private var reflowedContent: some View {
        VStack(alignment: .leading, spacing: reflowedSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: identitySpacing) {
                    rankRail
                    avatar
                    Spacer(minLength: 0)
                    score.fixedSize()
                }
                VStack(alignment: .leading, spacing: reflowedSpacing) {
                    HStack(alignment: .top, spacing: identitySpacing) {
                        rankRail
                        avatar
                    }
                    score.fixedSize(horizontal: false, vertical: true)
                }
            }

            VStack(alignment: .leading, spacing: identityTextSpacing) {
                Text(poolGamblerScore.gamblerUsername)
                    .font(.body.weight(.semibold))
                    .fixedSize(horizontal: false, vertical: true)

                youMarker
            }
            .loadedForeground(rowForeground, isPlaceholder: isPlaceholder)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, verticalPadding)
    }

    private var avatar: some View {
        AccountAvatar(
            accountId: avatarAccountId,
            fallback: avatarFallback,
            isPlaceholder: isPlaceholder
        )
        .frame(width: avatarSize, height: avatarSize)
        .clipShape(Circle())
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private var youMarker: some View {
        if isCurrentUser {
            Text(.leaderboardYou)
                .font(.caption)
        }
    }

    private var score: some View {
        Text(poolGamblerScore.score.map(String.init) ?? "—")
            .font(.title2.weight(.bold))
            .monospacedDigit()
            .loadedForeground(rowForeground, isPlaceholder: isPlaceholder)
    }

    private var rankRail: some View {
        VStack(spacing: rankSpacing) {
            PostionIndicator(
                position: poolGamblerScore.position,
                shouldUsePrimeryColor: false,
                size: rankTileSize,
                cornerRadius: rankCornerRadius,
                backgroundColor: isCurrentUser
                    ? Color(sharedResource: .currentUserContainer)
                    : Color(sharedResource: .surfaceVariant),
                backgroundOverlayColor: isCurrentUser
                    ? Color(sharedResource: .currentUser).opacity(currentUserTileOverlayOpacity)
                    : nil,
                foregroundColor: isCurrentUser
                    ? Color(sharedResource: .onCurrentUserContainer)
                    : Color(sharedResource: .onSurfaceVariant),
                font: .title3.weight(.semibold),
                isPlaceholder: isPlaceholder
            )

            TrendIndicator(
                movement: poolGamblerScore.movement,
                textStyle: .caption2,
                isPlaceholder: isPlaceholder
            )
            .frame(minHeight: movementHeight, alignment: .top)
        }
        .modifier(RankRailWidth(
            hasLabel: poolGamblerScore.movement.hasLabel,
            tileSize: rankTileSize,
            labelWidth: rankRailMaximumWidth
        ))
    }

    /// `AccountAvatar(isPlaceholder:)` already skips the request; the empty id additionally keeps
    /// the synthetic placeholder identity from ever reaching the avatar URL builder.
    var avatarAccountId: String {
        isPlaceholder ? "" : poolGamblerScore.gamblerId
    }

    private var avatarFallback: AccountAvatarFallback {
        AccountAvatarFallback(
            identity: poolGamblerScore.gamblerUsername,
            colorKey: poolGamblerScore.gamblerUsername,
            backgroundColor: isCurrentUser
                ? Color(sharedResource: .currentUser)
                : nil,
            foregroundColor: isCurrentUser
                ? Color(sharedResource: .onCurrentUser)
                : nil
        )
    }

    var rowBackground: Color {
        isCurrentUser && !isPlaceholder
            ? Color(sharedResource: .currentUserContainer)
            : Color.clear
    }

    private var rowForeground: Color {
        isCurrentUser
            ? Color(sharedResource: .onCurrentUserContainer)
            : Color(sharedResource: .onSurface)
    }

    /// The exact string handed to `.accessibilityLabel` in `body`. A placeholder row
    /// contributes no VoiceOver text, so it resolves to the empty string.
    var accessibilityLabel: String {
        isPlaceholder ? "" : loadedAccessibilityLabel
    }

    private var loadedAccessibilityLabel: String {
        [
            rankAccessibilityText,
            poolGamblerScore.gamblerUsername,
            isCurrentUser
                ? String(localized: .leaderboardYou)
                : nil,
            scoreAccessibilityText,
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

    private var movementAccessibilityText: String {
        poolGamblerScore.movement.accessibilityText
    }
}

/// The rank rail's width. A movement with a qualifier or state label gets a wider definite width,
/// so the label wraps on word boundaries and reports its full height. A plain movement is as wide
/// as the tile, or as its symbol and number when large text makes them wider, so they never clip.
struct RankRailWidth: ViewModifier {
    let hasLabel: Bool
    let tileSize: CGFloat
    let labelWidth: CGFloat

    func body(content: Content) -> some View {
        if hasLabel {
            content.frame(width: labelWidth)
        } else {
            content
                .frame(minWidth: tileSize)
                .fixedSize()
        }
    }
}

private let reflowedSpacing: CGFloat = 8
private let rowMinimumHeight: CGFloat = 82
private let rankTileSize: CGFloat = 44
private let rankCornerRadius: CGFloat = 10
private let avatarSize: CGFloat = 40
private let identitySpacing: CGFloat = 12
private let identityTextSpacing: CGFloat = 2
private let rankSpacing: CGFloat = 2
private let movementHeight: CGFloat = 16
private let scoreMinimumWidth: CGFloat = 52
private let horizontalPadding: CGFloat = 16
private let verticalPadding: CGFloat = 10
private let currentUserTileOverlayOpacity = 0.14

#Preview("Current user") {
    GamblerScoreItem(
        poolGamblerScore: poolGamblerScoreDummyModel(),
        isCurrentUser: true
    )
    .padding()
}

#Preview("Another gambler") {
    GamblerScoreItem(
        poolGamblerScore: poolGamblerScoreDummyModel(),
        isCurrentUser: false
    )
    .padding()
}

#Preview("Missing position data") {
    GamblerScoreItem(
        poolGamblerScore: poolGamblerScoreDummyModelWithoutPositionData(),
        isCurrentUser: false
    )
    .padding()
}

#Preview("Trend states") {
    GamblerScoreItemTrendStatesPreview()
}

#Preview("Trend states, accessibility text") {
    GamblerScoreItemTrendStatesPreview()
        .environment(\.dynamicTypeSize, .accessibility3)
}

#Preview("Placeholder") {
    GamblerScoreItem(
        poolGamblerScore: poolGamblerScorePlaceholderModel(),
        isCurrentUser: false,
        isPlaceholder: true
    )
    .padding()
}

#Preview("Loaded and placeholder, light") {
    GamblerScoreItemGeometryPreview()
        .preferredColorScheme(.light)
}

#Preview("Loaded and placeholder, dark") {
    GamblerScoreItemGeometryPreview()
        .preferredColorScheme(.dark)
}

/// Loaded and placeholder rows stacked for geometry comparison.
private struct GamblerScoreItemGeometryPreview: View {
    var body: some View {
        VStack(spacing: 0) {
            GamblerScoreItem(poolGamblerScore: poolGamblerScoreDummyModel(), isCurrentUser: false)
            Divider()
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScorePlaceholderModel(),
                isCurrentUser: false,
                isPlaceholder: true
            )
            Divider()
        }
        .padding(.horizontal)
        .background(Color(.systemBackground))
    }
}

/// Complete, partial, pending, and unavailable trend-group movement.
private struct GamblerScoreItemTrendStatesPreview: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                ForEach(poolGamblerScoreTrendStateDummyModels()) { score in
                    GamblerScoreItem(poolGamblerScore: score, isCurrentUser: score.gamblerId == "trend-partial")
                    Divider()
                }
            }
            .padding(.horizontal)
        }
    }
}
