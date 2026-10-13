import SwiftUI
import Core
import UI

/// One match in History or another gambler's Timeline: both teams around the score, and the
/// owner's bet with its points. Its kickoff group's header shows the date and time, so the row
/// does not repeat them visually; its announcement still starts with them, so a row focused
/// directly is understandable on its own. Loading slots render this same component from a
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
            teamsAndResult
            footer
        }
        .padding(.vertical, verticalPadding)
    }

    // MARK: Teams and result

    /// Home on the left and away on the right of an intrinsic-width result; both team columns
    /// share the remaining width, which keeps the result centered. At accessibility text sizes,
    /// or when either name's widest word cannot fit its column, the teams stack around the
    /// result instead, so names wrap between words rather than inside one.
    @ViewBuilder
    private var teamsAndResult: some View {
        if dynamicTypeSize.isAccessibilitySize {
            stackedMatchup
        } else {
            ViewThatFits(in: .horizontal) {
                matchup
                stackedMatchup
            }
        }
    }

    private var matchup: some View {
        HStack(alignment: .center, spacing: minimumGap) {
            teamColumn(code: poolGamblerBet.homeTeamId, name: poolGamblerBet.homeTeamName)
            result
            teamColumn(code: poolGamblerBet.awayTeamId, name: poolGamblerBet.awayTeamName)
        }
    }

    /// The result sits between the home and away teams vertically, and each team line gets the
    /// full width.
    private var stackedMatchup: some View {
        VStack(alignment: .leading, spacing: sectionSpacing) {
            teamLine(code: poolGamblerBet.homeTeamId, name: poolGamblerBet.homeTeamName)
            result
                .frame(maxWidth: .infinity)
            teamLine(code: poolGamblerBet.awayTeamId, name: poolGamblerBet.awayTeamName)
        }
    }

    /// Both columns reserve the widest word of either name, so the three-column layout is only
    /// chosen when every word fits the narrower of the two equal columns.
    private func teamColumn(code: String, name: String) -> some View {
        VStack(spacing: flagNameSpacing) {
            flag(code)
            wordWrappedName(name, reservingWordsOf: [poolGamblerBet.homeTeamName, poolGamblerBet.awayTeamName])
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    /// The flag sits beside the name while the name's widest word fits next to it; otherwise the
    /// flag goes above the name, which then takes the whole width. A single word wider than the
    /// whole width still wraps inside itself, since the text is never shrunk.
    private func teamLine(code: String, name: String) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: flagNameSpacing * 2) {
                flag(code)
                wordWrappedName(name, reservingWordsOf: [name])
            }
            VStack(alignment: .leading, spacing: flagNameSpacing) {
                flag(code)
                teamName(name)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    /// The team name, whose ideal width is that of the widest word in `names` rather than of the
    /// whole name on one line; `ViewThatFits` therefore accepts a layout when the name can wrap
    /// between words. The hidden words carry no text for assistive technology: the row is one
    /// accessibility element with its own label.
    private func wordWrappedName(_ name: String, reservingWordsOf names: [String]) -> some View {
        ZStack(alignment: .leading) {
            ForEach(Set(names.flatMap(historyNameWords)).sorted(), id: \.self) { word in
                Text(verbatim: word)
                    .font(teamNameFont)
                    .fixedSize()
                    .hidden()
            }
            teamName(name)
                .frame(minWidth: 0, idealWidth: 0, maxWidth: .infinity, alignment: .leading)
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
            .font(teamNameFont)
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
    }

    private var teamNameFont: Font {
        .body.weight(.semibold)
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
            // The label keeps its words whole: when it does not fit beside the prediction, the
            // prediction moves below it and the label wraps across the full width.
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: betLabelSpacing) {
                    betLabelText
                    betScoreText(betScore)
                }
                VStack(alignment: .leading, spacing: betLabelStackedSpacing) {
                    betLabelText
                    betScoreText(betScore)
                }
            }
            .font(.body)
        } else {
            Text(.historyNoBetLabel)
                .font(.body)
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
        }
    }

    private var betLabelText: some View {
        Text(betLabel)
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
    }

    private func betScoreText(_ betScore: TeamScore<Int>) -> some View {
        Text(verbatim: "\(betScore.homeTeamValue) – \(betScore.awayTeamValue)")
            .fontWeight(.semibold)
            .monospacedDigit()
            .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
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
        HistoryPointsPill(text: pointsPillText, isPositive: isPositiveAward, isPlaceholder: isPlaceholder)
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

/// The words a team name can wrap between.
func historyNameWords(_ name: String) -> [String] {
    name.split(whereSeparator: \.isWhitespace).map(String.init)
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
private let betLabelStackedSpacing: CGFloat = 2

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
