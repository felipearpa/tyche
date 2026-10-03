import SwiftUI
import Core
import UI

/// One History match: the date and time, both teams around the final score, and the
/// signed-in gambler's bet with the awarded points. Loading slots render this same component from
/// a placeholder model with `isPlaceholder: true`: native redaction under the shared
/// `LoadingPlaceholderPulse` conceals its content, and it ignores touches and stays out of the
/// accessibility tree.
///
/// Another gambler's timeline keeps `FinishedBetItem`; this row's "Your bet" wording belongs to
/// the signed-in gambler's History only.
struct HistoryBetItem: View {
    let poolGamblerBet: PoolGamblerBetModel
    let isPlaceholder: Bool
    let dateFormat: HistoryMatchDateFormat

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var scoreSize: CGFloat = 44
    @ScaledMetric(relativeTo: .body) private var flagWidth: CGFloat = 36

    init(
        poolGamblerBet: PoolGamblerBetModel,
        isPlaceholder: Bool = false,
        dateFormat: HistoryMatchDateFormat = HistoryMatchDateFormat()
    ) {
        self.poolGamblerBet = poolGamblerBet
        self.isPlaceholder = isPlaceholder
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
                Text(.historyYourBetLabel)
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

    private var awardedPoints: HistoryPoints {
        HistoryPoints(score: poolGamblerBet.score)
    }

    private var pointsPill: some View {
        Text(historyPointsShortText(awardedPoints))
            .font(.body.weight(.semibold))
            .monospacedDigit()
            .lineLimit(1)
            .fixedSize()
            .loadedForeground(
                awardedPoints.isPositive ? HistoryStyle.positiveText : HistoryStyle.secondaryText,
                isPlaceholder: isPlaceholder
            )
            .padding(.horizontal, pillHorizontalPadding)
            .padding(.vertical, pillVerticalPadding)
            .background(
                Capsule().fill(awardedPoints.isPositive ? HistoryStyle.positiveFill : HistoryStyle.neutralFill)
            )
    }

    // MARK: Text

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

    private var resultAccessibilityText: String {
        let home = poolGamblerBet.homeTeamName
        let away = poolGamblerBet.awayTeamName
        guard let matchScore = poolGamblerBet.matchScore else {
            return String(localized: .historyResultUnavailableAccessibility(home, away))
        }
        return String(
            localized: .historyResultAccessibility(
                home,
                matchScore.homeTeamValue,
                away,
                matchScore.awayTeamValue
            )
        )
    }

    private var betAccessibilityText: String {
        guard let betScore = poolGamblerBet.betScore else {
            return String(localized: .historyNoBetAccessibility)
        }
        return String(localized: .historyBetAccessibility(betScore.homeTeamValue, betScore.awayTeamValue))
    }

    private var pointsAccessibilityText: String {
        switch awardedPoints {
        case .earned(let value):
            return String(localized: .historyPointsAccessibility(value))
        case .unavailable:
            return String(localized: .historyPointsUnavailableAccessibility)
        }
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
