import Core
import SwiftUI
import UI

/// The body of the match screen while betting is open: a neutral closed lock, the hidden-bets
/// heading and explanation, and the signed-in viewer's own prediction. Nobody else's prediction
/// appears here.
struct MatchHiddenBetsContent: View {
    let match: PoolGamblerBetModel
    let ownBet: OwnBetState
    let onOwnBetRetry: () -> Void

    @ScaledMetric(relativeTo: .largeTitle) private var lockSize: CGFloat = 48
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: groupSpacing) {
            VStack(spacing: boxSpacing.medium) {
                Image(sharedResource: .lock)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: min(lockSize, maximumLockSize), height: min(lockSize, maximumLockSize))
                    .foregroundStyle(HistoryStyle.secondaryText)
                    .accessibilityHidden(true)

                VStack(spacing: boxSpacing.small) {
                    Text(.matchBetsHiddenTitle)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(HistoryStyle.primaryText)

                    Text(.matchBetsHiddenMessage)
                        .font(.body)
                        .foregroundStyle(HistoryStyle.secondaryText)
                }
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityElement(children: .combine)
            }

            MatchOwnBetRegion(match: match, state: ownBet, onRetry: onOwnBetRetry)
        }
        .frame(maxWidth: .infinity)
    }
}

/// The viewer's own prediction while bets are hidden. Loading renders the production line from
/// a placeholder model; a failure offers a retry that requests only this prediction; a confirmed
/// absence says "No bet". None of these is shown as another.
struct MatchOwnBetRegion: View {
    let match: PoolGamblerBetModel
    let state: OwnBetState
    let onRetry: () -> Void

    var body: some View {
        switch state {
        case .loading:
            MatchOwnBetLine(match: match, betScore: matchOwnBetPlaceholderScore, isPlaceholder: true)
        case .bet(let betScore):
            MatchOwnBetLine(match: match, betScore: betScore)
        case .noBet:
            MatchOwnBetLine(match: match, betScore: nil)
        case .failed:
            HStack(spacing: 4) {
                Text(.matchBetsOwnBetFailure)
                    .font(.subheadline)
                    .foregroundStyle(HistoryStyle.secondaryText)
                    .fixedSize(horizontal: false, vertical: true)
                HistoryPointsRetryButton(action: onRetry)
            }
        }
    }
}

/// "Your bet" with the viewer's prediction, or "No bet" after a confirmed absence.
struct MatchOwnBetLine: View {
    let match: PoolGamblerBetModel
    let betScore: TeamScore<Int>?
    let isPlaceholder: Bool

    init(match: PoolGamblerBetModel, betScore: TeamScore<Int>?, isPlaceholder: Bool = false) {
        self.match = match
        self.betScore = betScore
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        HStack(alignment: .firstTextBaseline, spacing: labelSpacing) {
            Text(.historyYourBetLabel)
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
            if let betScore {
                Text(verbatim: "\(betScore.homeTeamValue) – \(betScore.awayTeamValue)")
                    .fontWeight(.semibold)
                    .monospacedDigit()
                    .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
            } else {
                Text(.historyNoBetLabel)
                    .fontWeight(.semibold)
                    .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
            }
        }
        .font(.body)
        .multilineTextAlignment(.center)
        .fixedSize(horizontal: false, vertical: true)
    }

    /// The exact string handed to `.accessibilityLabel`; a placeholder contributes none.
    var accessibilityLabel: String {
        guard !isPlaceholder else { return "" }
        guard let betScore else {
            return String(localized: .historyNoBetAccessibility)
        }
        return String(
            localized: .matchBetsOwnBetAccessibility(
                match.homeTeamName,
                betScore.homeTeamValue,
                match.awayTeamName,
                betScore.awayTeamValue
            )
        )
    }
}

/// Stable filler for the own prediction while it loads; never shown as real content.
let matchOwnBetPlaceholderScore = TeamScore(homeTeamValue: 0, awayTeamValue: 0)

private let groupSpacing: CGFloat = 24
private let labelSpacing: CGFloat = 8
private let maximumLockSize: CGFloat = 80

#Preview("Own bet states") {
    let match = matchHeaderPreviewCases()[0].1
    VStack(spacing: 48) {
        MatchHiddenBetsContent(match: match, ownBet: .bet(TeamScore(homeTeamValue: 3, awayTeamValue: 2)), onOwnBetRetry: {})
        MatchOwnBetRegion(match: match, state: .noBet, onRetry: {})
        MatchOwnBetRegion(match: match, state: .loading, onRetry: {})
        MatchOwnBetRegion(match: match, state: .failed, onRetry: {})
    }
    .historyHorizontalGutter()
}
