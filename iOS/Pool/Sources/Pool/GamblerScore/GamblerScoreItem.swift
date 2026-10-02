import Account
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

    private var scoreContent: some View {
        HStack(spacing: identitySpacing) {
            rankRail

            AccountAvatar(
                accountId: avatarAccountId,
                fallback: avatarFallback,
                isPlaceholder: isPlaceholder
            )
            .frame(width: avatarSize, height: avatarSize)
            .clipShape(Circle())
            .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: identityTextSpacing) {
                Text(poolGamblerScore.gamblerUsername)
                    .font(.body.weight(.semibold))
                    .lineLimit(1)
                    .truncationMode(.tail)

                if isCurrentUser {
                    Text(.leaderboardYou)
                        .font(.caption)
                }
            }
            .loadedForeground(rowForeground, isPlaceholder: isPlaceholder)
            .frame(maxWidth: .infinity, alignment: .leading)
            .layoutPriority(1)

            Text(poolGamblerScore.score.map(String.init) ?? "—")
                .font(.title2.weight(.bold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .loadedForeground(rowForeground, isPlaceholder: isPlaceholder)
                .frame(minWidth: scoreMinimumWidth, alignment: .trailing)
        }
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, verticalPadding)
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

            Group {
                if let difference = poolGamblerScore.rank() {
                    TrendIndicator(
                        difference: difference,
                        textStyle: .caption2,
                        isPlaceholder: isPlaceholder
                    )
                } else {
                    Color.clear
                }
            }
            .frame(height: movementHeight)
        }
        .frame(width: rankTileSize)
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
