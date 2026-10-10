import Account
import Core
import SwiftUI
import UI

/// One participant in the match's ranked list: the shared account avatar, their name, their
/// prediction or "No bet", and, once the match is computed, this match's award. The award is the
/// match's points, never the pool total that orders the list.
///
/// The signed-in gambler's row adds the "You" marker and the current-user highlight in its ranked
/// position. Loading slots render this same component from `matchGamblerBetPlaceholderModel()`
/// with `isPlaceholder: true`: it then conceals its content with the shared pulse, requests no
/// avatar, ignores touches, and stays out of the accessibility tree.
struct MatchGamblerBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let isCurrentUser: Bool
    let isPlaceholder: Bool

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var avatarSize: CGFloat = 40

    init(poolGamblerBet: PoolGamblerBetModel, isCurrentUser: Bool = false, isPlaceholder: Bool = false) {
        self.poolGamblerBet = poolGamblerBet
        self.isCurrentUser = isCurrentUser
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { content }
            } else {
                content
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(rowBackground)
        .allowsHitTesting(!isPlaceholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                stackedContent
            } else {
                regularContent
            }
        }
        .padding(.vertical, verticalPadding)
        .historyHorizontalGutter()
    }

    /// Avatar, name, prediction, and award on one line; long names wrap.
    private var regularContent: some View {
        HStack(alignment: .center, spacing: columnSpacing) {
            avatar
            identity
                .frame(maxWidth: .infinity, alignment: .leading)
            prediction
            if let award = award(reservesWidth: true) {
                award
            }
        }
    }

    /// At accessibility text sizes the prediction and award move below the name.
    private var stackedContent: some View {
        HStack(alignment: .top, spacing: columnSpacing) {
            avatar
            VStack(alignment: .leading, spacing: stackedSpacing) {
                identity
                // The award moves below the prediction when both no longer fit on one line.
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .center, spacing: columnSpacing) {
                        prediction
                        Spacer(minLength: columnSpacing)
                        if let award = award(reservesWidth: false) {
                            award
                        }
                    }
                    VStack(alignment: .leading, spacing: stackedSpacing) {
                        prediction
                        if let award = award(reservesWidth: false) {
                            award
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    // MARK: Regions

    private var avatar: some View {
        let size = min(avatarSize, maximumAvatarSize)
        return AccountAvatar(
            accountId: avatarAccountId,
            // The leaderboard's fallback, so the letter avatar keeps the same letter and color.
            fallback: AccountAvatarFallback(
                identity: poolGamblerBet.gamblerUsername,
                colorKey: poolGamblerBet.gamblerUsername,
                backgroundColor: isCurrentUser ? Color(sharedResource: .currentUser) : nil,
                foregroundColor: isCurrentUser ? Color(sharedResource: .onCurrentUser) : nil
            ),
            isPlaceholder: isPlaceholder
        )
        .frame(width: size, height: size)
        .clipShape(Circle())
        .accessibilityHidden(true)
    }

    /// `AccountAvatar(isPlaceholder:)` already skips the request; the empty id additionally keeps
    /// the placeholder identity from ever reaching the avatar URL builder.
    var avatarAccountId: String {
        isPlaceholder ? "" : poolGamblerBet.gamblerId
    }

    private var identity: some View {
        VStack(alignment: .leading, spacing: identitySpacing) {
            Text(verbatim: breakableName(poolGamblerBet.gamblerUsername))
                .font(.body)
                .fixedSize(horizontal: false, vertical: true)
                .loadedForeground(primaryForeground, isPlaceholder: isPlaceholder)
            if isCurrentUser {
                Text(.matchBetsYouLabel)
                    .font(.caption.weight(.semibold))
                    .loadedForeground(primaryForeground, isPlaceholder: isPlaceholder)
            }
        }
    }

    @ViewBuilder
    private var prediction: some View {
        if let betScore = poolGamblerBet.betScore {
            Text(verbatim: "\(betScore.homeTeamValue) – \(betScore.awayTeamValue)")
                .font(.body.weight(.semibold))
                .monospacedDigit()
                .lineLimit(1)
                .fixedSize()
                .loadedForeground(primaryForeground, isPlaceholder: isPlaceholder)
        } else {
            Text(.historyNoBetLabel)
                .font(.body)
                .lineLimit(1)
                .fixedSize()
                .loadedForeground(secondaryForeground, isPlaceholder: isPlaceholder)
        }
    }

    /// This match's award once computed; an uncomputed match shows no award. On one line the
    /// pill reserves a common width so the column lines up; stacked, it takes only its own.
    private func award(reservesWidth: Bool) -> HistoryPointsPill? {
        guard poolGamblerBet.isComputed else { return nil }
        let points = HistoryPoints(score: poolGamblerBet.score)
        return HistoryPointsPill(
            text: historyPointsShortText(points),
            isPositive: points.isPositive,
            surface: isCurrentUser ? .currentUserRow : .canvas,
            reservedText: reservesWidth ? String(localized: .historyPositivePointsShort(reservedAwardValue)) : nil,
            isPlaceholder: isPlaceholder
        )
    }

    // MARK: Colors

    private var rowBackground: Color {
        isCurrentUser && !isPlaceholder ? Color(sharedResource: .currentUserContainer) : .clear
    }

    private var primaryForeground: Color {
        isCurrentUser ? Color(sharedResource: .onCurrentUserContainer) : HistoryStyle.primaryText
    }

    private var secondaryForeground: Color {
        isCurrentUser ? Color(sharedResource: .onCurrentUserContainer) : HistoryStyle.secondaryText
    }

    // MARK: Accessibility

    /// The exact string handed to `.accessibilityLabel`; a placeholder contributes none.
    var accessibilityLabel: String {
        guard !isPlaceholder else { return "" }
        return [
            poolGamblerBet.gamblerUsername,
            isCurrentUser ? String(localized: .matchBetsYouLabel) : nil,
            predictionAccessibilityText,
            awardAccessibilityText,
        ]
        .compactMap { $0 }
        .joined(separator: ", ")
    }

    private var predictionAccessibilityText: String {
        guard let betScore = poolGamblerBet.betScore else {
            return String(localized: .historyNoBetLabel)
        }
        return String(
            localized: .matchBetsParticipantBetAccessibility(
                poolGamblerBet.homeTeamName,
                betScore.homeTeamValue,
                poolGamblerBet.awayTeamName,
                betScore.awayTeamValue
            )
        )
    }

    private var awardAccessibilityText: String? {
        guard poolGamblerBet.isComputed else { return nil }
        switch HistoryPoints(score: poolGamblerBet.score) {
        case .earned(let value):
            return String(localized: .historyPointsAccessibility(value))
        case .unavailable:
            return String(localized: .historyPointsUnavailableAccessibility)
        }
    }
}

/// Stable filler for loading participant rows, sized like a typical row; never shown as real
/// content.
func matchGamblerBetPlaceholderModel(isComputed: Bool = false) -> PoolGamblerBetModel {
    poolGamblerBetPlaceholderModel(isLocked: true, isComputed: isComputed).copy {
        $0.gamblerUsername = String(repeating: "X", count: 14)
        $0.betScore = TeamScore(homeTeamValue: 0, awayTeamValue: 0)
        $0.score = 0
    }
}

/// Display names are often email addresses. A line break opportunity after "@" lets a long
/// address wrap there instead of being hyphenated mid-word. Spoken text uses the plain name.
func breakableName(_ name: String) -> String {
    name.replacingOccurrences(of: "@", with: "@\u{200B}")
}

/// The widest award the pill column reserves, so pills line up in most rows.
private let reservedAwardValue = 88
private let verticalPadding: CGFloat = 12
private let columnSpacing: CGFloat = 12
private let stackedSpacing: CGFloat = 8
private let identitySpacing: CGFloat = 2
private let maximumAvatarSize: CGFloat = 64

#Preview("Computed") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(matchParticipantPreviewModels(isComputed: true)) { bet in
                MatchGamblerBetItem(poolGamblerBet: bet, isCurrentUser: bet.gamblerId == matchPreviewViewerId)
                Divider().historyHorizontalGutter()
            }
            MatchGamblerBetItem(poolGamblerBet: matchGamblerBetPlaceholderModel(isComputed: true), isPlaceholder: true)
        }
    }
}

#Preview("Bets closed, accessibility text") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(matchParticipantPreviewModels(isComputed: false)) { bet in
                MatchGamblerBetItem(poolGamblerBet: bet, isCurrentUser: bet.gamblerId == matchPreviewViewerId)
                Divider().historyHorizontalGutter()
            }
        }
    }
    .environment(\.dynamicTypeSize, .accessibility3)
}
