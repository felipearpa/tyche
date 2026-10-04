import SwiftUI
import Core
import UI

/// One match in History or another gambler's Timeline: the date and time, both teams around the
/// score, and the owner's bet with its points. Loading slots render this same component from a
/// placeholder model with `isPlaceholder: true`: native redaction under the shared
/// `LoadingPlaceholderPulse` conceals its content, and it ignores touches and stays out of the
/// accessibility tree.
///
/// `owner` selects the ownership wording; it defaults to the signed-in gambler's "Your bet". A
/// computed entry shows the final score and the awarded points. An entry whose points are not
/// computed yet shows the match score available so far (or dashes) and "Points pending", and
/// never describes that score as final or the match as live.
struct HistoryBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let isPlaceholder: Bool
    let owner: HistoryOwner
    let dateFormat: HistoryMatchDateFormat

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var scoreSize: CGFloat = 44
    @ScaledMetric(relativeTo: .body) private var flagWidth: CGFloat = 36

    init(
        poolGamblerBet: PoolGamblerBetModel,
        isPlaceholder: Bool = false,
        owner: HistoryOwner = .signedInGambler,
        dateFormat: HistoryMatchDateFormat = HistoryMatchDateFormat()
    ) {
        self.poolGamblerBet = poolGamblerBet
        self.isPlaceholder = isPlaceholder
        self.owner = owner
        self.dateFormat = dateFormat
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { content }
            } else {
                content
            }
        }
        .allowsHitTesting(!isPlaceholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        VStack(spacing: sectionSpacing) {
            dateTimeLine
            if dynamicTypeSize.isAccessibilitySize {
                stackedMatchup
            } else {
                matchup
            }
            footer
        }
        .padding(.vertical, verticalPadding)
    }

    // MARK: Date and time

    private var dateTimeLine: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline) {
                Text(dateText)
                Spacer(minLength: minimumGap)
                Text(timeText)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(dateText)
                Text(timeText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .font(.subheadline)
        .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
    }

    // MARK: Teams and result

    /// Home on the left and away on the right of an intrinsic-width result; both team columns
    /// share the remaining width, which keeps the result centered.
    private var matchup: some View {
        HStack(alignment: .center, spacing: minimumGap) {
            teamColumn(code: poolGamblerBet.homeTeamId, name: poolGamblerBet.homeTeamName)
            result
            teamColumn(code: poolGamblerBet.awayTeamId, name: poolGamblerBet.awayTeamName)
        }
    }

    /// Accessibility text sizes leave no room for three columns, so the result sits between the
    /// home and away teams vertically.
    private var stackedMatchup: some View {
        VStack(alignment: .leading, spacing: sectionSpacing) {
            teamLine(code: poolGamblerBet.homeTeamId, name: poolGamblerBet.homeTeamName)
            result
                .frame(maxWidth: .infinity)
            teamLine(code: poolGamblerBet.awayTeamId, name: poolGamblerBet.awayTeamName)
        }
    }

    private func teamColumn(code: String, name: String) -> some View {
        VStack(spacing: flagNameSpacing) {
            flag(code)
            teamName(name)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func teamLine(code: String, name: String) -> some View {
        HStack(spacing: flagNameSpacing * 2) {
            flag(code)
            teamName(name)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    /// Flags grow with text size up to a cap, leaving the width to the team names.
    private func flag(_ code: String) -> some View {
        let width = min(flagWidth, maximumFlagWidth)
        return FlagImage(teamCode: code)
            .frame(width: width, height: width * flagAspectRatio)
            .accessibilityHidden(true)
    }

    private func teamName(_ name: String) -> some View {
        Text(name)
            .font(.body.weight(.semibold))
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
    }

    private var result: some View {
        // Centered, so the restrained separator sits midway up the digits.
        HStack(alignment: .center, spacing: scoreSpacing) {
            Text(scoreText(poolGamblerBet.matchScore?.homeTeamValue))
                .loadedForeground(resultForeground, isPlaceholder: isPlaceholder)
            Text(verbatim: "–")
                .font(.system(size: scoreSize * 0.6, weight: .regular))
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
            Text(scoreText(poolGamblerBet.matchScore?.awayTeamValue))
                .loadedForeground(resultForeground, isPlaceholder: isPlaceholder)
        }
        .font(.system(size: scoreSize, weight: .bold))
        .monospacedDigit()
        .lineLimit(1)
        .fixedSize()
    }

    /// An unavailable result shows neutral dashes rather than a score.
    private var resultForeground: Color {
        poolGamblerBet.matchScore == nil ? HistoryStyle.secondaryText : HistoryStyle.primaryText
    }

    // MARK: Bet and points

    private var footer: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center) {
                betLine
                Spacer(minLength: minimumGap)
                pointsPill
            }
            VStack(alignment: .leading, spacing: sectionSpacing) {
                betLine
                pointsPill
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder
    private var betLine: some View {
        if let betScore = poolGamblerBet.betScore {
            HStack(alignment: .firstTextBaseline, spacing: betLabelSpacing) {
                Text(betLabel)
                    .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
                Text(verbatim: "\(betScore.homeTeamValue) – \(betScore.awayTeamValue)")
                    .fontWeight(.semibold)
                    .monospacedDigit()
                    .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
            }
            .font(.body)
        } else {
            Text(.historyNoBetLabel)
                .font(.body)
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
        }
    }

    private var betLabel: LocalizedStringResource {
        switch owner {
        case .signedInGambler: .historyYourBetLabel
        case .selectedGambler: .historyBetLabel
        }
    }

    private var rowPoints: HistoryRowPoints {
        HistoryRowPoints(poolGamblerBet)
    }

    private var awardedPoints: HistoryPoints? {
        if case .awarded(let points) = rowPoints { return points }
        return nil
    }

    private var isPositiveAward: Bool {
        awardedPoints?.isPositive == true
    }

    private var pointsPill: some View {
        Text(pointsPillText)
            .font(.body.weight(.semibold))
            .monospacedDigit()
            .lineLimit(1)
            .fixedSize()
            .loadedForeground(
                isPositiveAward ? HistoryStyle.positiveText : HistoryStyle.secondaryText,
                isPlaceholder: isPlaceholder
            )
            .padding(.horizontal, pillHorizontalPadding)
            .padding(.vertical, pillVerticalPadding)
            .background(
                Capsule().fill(isPositiveAward ? HistoryStyle.positiveFill : HistoryStyle.neutralFill)
            )
    }

    // MARK: Text

    private var pointsPillText: String {
        switch rowPoints {
        case .awarded(let points): historyPointsShortText(points)
        case .pending: String(localized: .historyPointsPendingLabel)
        }
    }

    private var dateText: String {
        dateFormat.date(poolGamblerBet.matchDateTime)
    }

    private var timeText: String {
        dateFormat.time(poolGamblerBet.matchDateTime)
    }

    private func scoreText(_ value: Int?) -> String {
        value.map(String.init) ?? "—"
    }

    /// The exact string handed to `.accessibilityLabel`. A placeholder row contributes no
    /// VoiceOver text, so it resolves to the empty string.
    var accessibilityLabel: String {
        guard !isPlaceholder else { return "" }
        return [
            "\(dateText), \(timeText)",
            resultAccessibilityText,
            betAccessibilityText,
            pointsAccessibilityText,
        ]
        .joined(separator: ". ")
    }

    /// A computed entry's score is the final result; any other score is only the match score
    /// reported so far.
    private var resultAccessibilityText: String {
        let home = poolGamblerBet.homeTeamName
        let away = poolGamblerBet.awayTeamName
        let isFinal = poolGamblerBet.isComputed
        guard let matchScore = poolGamblerBet.matchScore else {
            return isFinal
                ? String(localized: .historyResultUnavailableAccessibility(home, away))
                : String(localized: .historyMatchScoreUnavailableAccessibility(home, away))
        }
        let homeValue = matchScore.homeTeamValue
        let awayValue = matchScore.awayTeamValue
        return isFinal
            ? String(localized: .historyResultAccessibility(home, homeValue, away, awayValue))
            : String(localized: .historyMatchScoreAccessibility(home, homeValue, away, awayValue))
    }

    private var betAccessibilityText: String {
        switch (owner, poolGamblerBet.betScore) {
        case (.signedInGambler, nil):
            String(localized: .historyNoBetAccessibility)
        case (.selectedGambler, nil):
            String(localized: .historyGamblerNoBetAccessibility)
        case (.signedInGambler, let betScore?):
            String(localized: .historyBetAccessibility(betScore.homeTeamValue, betScore.awayTeamValue))
        case (.selectedGambler, let betScore?):
            String(localized: .historyGamblerBetAccessibility(betScore.homeTeamValue, betScore.awayTeamValue))
        }
    }

    private var pointsAccessibilityText: String {
        switch rowPoints {
        case .awarded(.earned(let value)):
            String(localized: .historyPointsAccessibility(value))
        case .awarded(.unavailable):
            String(localized: .historyPointsUnavailableAccessibility)
        case .pending:
            String(localized: .historyPointsPendingLabel)
        }
    }
}

/// A row's points: the authoritative award once the entry is computed, otherwise pending. A
/// pending entry never shows a score value as an award.
enum HistoryRowPoints: Equatable {
    case awarded(HistoryPoints)
    case pending

    init(_ poolGamblerBet: PoolGamblerBetModel) {
        self = poolGamblerBet.isComputed ? .awarded(HistoryPoints(score: poolGamblerBet.score)) : .pending
    }
}

private let sectionSpacing: CGFloat = 12
private let verticalPadding: CGFloat = 20
private let minimumGap: CGFloat = 12
private let flagNameSpacing: CGFloat = 6
private let flagAspectRatio: CGFloat = 0.75
private let maximumFlagWidth: CGFloat = 52
private let scoreSpacing: CGFloat = 12
private let betLabelSpacing: CGFloat = 8
private let pillHorizontalPadding: CGFloat = 14
private let pillVerticalPadding: CGFloat = 6

#Preview("Loaded and placeholder") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(historyBetPreviewModels()) { bet in
                HistoryBetItem(poolGamblerBet: bet)
                Divider()
            }
            HistoryBetItem(poolGamblerBet: poolGamblerBetPlaceholderModel(isComputed: true), isPlaceholder: true)
        }
        .historyHorizontalGutter()
    }
}

#Preview("Selected gambler, pending and settled") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(timelineBetPreviewModels()) { bet in
                HistoryBetItem(poolGamblerBet: bet, owner: .selectedGambler(name: "El mono"))
                Divider()
            }
        }
        .historyHorizontalGutter()
    }
}

#Preview("Accessibility text") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(historyBetPreviewModels()) { bet in
                HistoryBetItem(poolGamblerBet: bet)
                Divider()
            }
        }
        .historyHorizontalGutter()
    }
    .environment(\.dynamicTypeSize, .accessibility3)
}
