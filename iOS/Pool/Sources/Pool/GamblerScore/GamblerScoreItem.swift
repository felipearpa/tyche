import Account
import Foundation
import SwiftUI
import UI

public struct GamblerScoreItem: View {
    let poolGamblerScore: PoolGamblerScoreModel
    let isCurrentUser: Bool
    var isPlaceholder = false

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
                placeholderContent
            } else {
                scoreContent
            }
        }
        .frame(maxWidth: .infinity, minHeight: rowMinimumHeight)
        .background(rowBackground)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isPlaceholder ? "" : accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    private var scoreContent: some View {
        HStack(spacing: identitySpacing) {
            rankRail

            AccountAvatar(
                accountId: poolGamblerScore.gamblerId,
                fallback: AccountAvatarFallback(
                    identity: poolGamblerScore.gamblerUsername,
                    colorKey: poolGamblerScore.gamblerUsername,
                    backgroundColor: isCurrentUser
                        ? Color(sharedResource: .currentUser)
                        : nil,
                    foregroundColor: isCurrentUser
                        ? Color(sharedResource: .onCurrentUser)
                        : nil
                )
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
                    Text(String(localized: "leaderboard_you", bundle: .module))
                        .font(.caption)
                }
            }
            .foregroundStyle(rowForeground)
            .frame(maxWidth: .infinity, alignment: .leading)
            .layoutPriority(1)

            Text(poolGamblerScore.score.map(String.init) ?? "—")
                .font(.title2.weight(.bold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .foregroundStyle(rowForeground)
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
                font: .title3.weight(.semibold)
            )

            Group {
                if let difference = poolGamblerScore.rank() {
                    TrendIndicator(
                        difference: difference,
                        textStyle: .caption2
                    )
                } else {
                    Color.clear
                }
            }
            .frame(height: movementHeight)
        }
        .frame(width: rankTileSize)
    }

    private var placeholderContent: some View {
        HStack(spacing: identitySpacing) {
            VStack(spacing: rankSpacing) {
                RoundedRectangle(cornerRadius: rankCornerRadius)
                    .fill(Color(sharedResource: .surfaceVariant))
                    .frame(width: rankTileSize, height: rankTileSize)
                    .shimmer()

                Capsule()
                    .fill(Color(sharedResource: .surfaceVariant))
                    .frame(width: movementPlaceholderWidth, height: movementPlaceholderHeight)
                    .shimmer()
            }

            Circle()
                .fill(Color(sharedResource: .surfaceVariant))
                .frame(width: avatarSize, height: avatarSize)
                .shimmer()

            RoundedRectangle(cornerRadius: placeholderCornerRadius)
                .fill(Color(sharedResource: .surfaceVariant))
                .frame(maxWidth: usernamePlaceholderWidth, minHeight: usernamePlaceholderHeight)
                .shimmer()

            Spacer(minLength: 0)

            RoundedRectangle(cornerRadius: placeholderCornerRadius)
                .fill(Color(sharedResource: .surfaceVariant))
                .frame(width: scorePlaceholderWidth, height: scorePlaceholderHeight)
                .shimmer()
        }
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, verticalPadding)
    }

    private var rowBackground: Color {
        isCurrentUser && !isPlaceholder
            ? Color(sharedResource: .currentUserContainer)
            : Color.clear
    }

    private var rowForeground: Color {
        isCurrentUser
            ? Color(sharedResource: .onCurrentUserContainer)
            : Color(sharedResource: .onSurface)
    }

    private var accessibilityLabel: String {
        [
            rankAccessibilityText,
            poolGamblerScore.gamblerUsername,
            isCurrentUser
                ? String(localized: "leaderboard_you", bundle: .module)
                : nil,
            scoreAccessibilityText,
            movementAccessibilityText,
        ]
        .compactMap { $0 }
        .joined(separator: ", ")
    }

    private var rankAccessibilityText: String {
        guard let position = poolGamblerScore.position else {
            return String(
                localized: "leaderboard_missing_rank_accessibility",
                bundle: .module
            )
        }
        return localizedFormat(
            "leaderboard_rank_accessibility",
            Int64(position)
        )
    }

    private var scoreAccessibilityText: String {
        guard let score = poolGamblerScore.score else {
            return String(
                localized: "leaderboard_missing_score_accessibility",
                bundle: .module
            )
        }
        return localizedFormat(
            "leaderboard_points_accessibility",
            Int64(score)
        )
    }

    private var movementAccessibilityText: String? {
        guard let difference = poolGamblerScore.rank() else { return nil }

        switch difference {
        case let value where value > 0:
            return localizedFormat(
                "leaderboard_movement_up_accessibility",
                Int64(abs(value))
            )
        case let value where value < 0:
            return localizedFormat(
                "leaderboard_movement_down_accessibility",
                Int64(abs(value))
            )
        default:
            return String(
                localized: "leaderboard_movement_unchanged_accessibility",
                bundle: .module
            )
        }
    }
}

private func localizedFormat(_ key: String, _ value: CVarArg) -> String {
    String(
        format: String(localized: String.LocalizationValue(key), bundle: .module),
        locale: .current,
        arguments: [value]
    )
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
private let movementPlaceholderWidth: CGFloat = 18
private let movementPlaceholderHeight: CGFloat = 6
private let usernamePlaceholderWidth: CGFloat = 144
private let usernamePlaceholderHeight: CGFloat = 16
private let scorePlaceholderWidth: CGFloat = 48
private let scorePlaceholderHeight: CGFloat = 24
private let placeholderCornerRadius: CGFloat = 4

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
