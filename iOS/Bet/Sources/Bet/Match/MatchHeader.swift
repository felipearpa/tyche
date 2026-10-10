import Core
import SwiftUI
import UI

/// The match heading at the top of the match screen's scrolling content: a status line, then both
/// teams around the center.
///
/// | Presentation | Status line | Center |
/// | --- | --- | --- |
/// | `hidden` | "Starts" pill with the kickoff date and time | VS |
/// | `betsClosed` | "Bets closed" pill | VS, or the match score reported so far |
/// | `computed` | Kickoff date and time | Prominent final score, or dashes when unavailable |
///
/// None of these claims that play is live. Loading renders this same component from
/// `matchHeaderPlaceholderModel()` with `isPlaceholder: true`: it then conceals its content with
/// the shared pulse, ignores touches, and stays out of the accessibility tree.
struct MatchHeader: View {
    let match: PoolGamblerBetModel
    let presentation: MatchBetsPresentation
    let isPlaceholder: Bool
    let dateFormat: HistoryMatchDateFormat

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var finalScoreSize: CGFloat = 56
    @ScaledMetric(relativeTo: .largeTitle) private var matchScoreSize: CGFloat = 40
    @ScaledMetric(relativeTo: .title) private var flagWidth: CGFloat = 52

    init(
        match: PoolGamblerBetModel,
        presentation: MatchBetsPresentation,
        isPlaceholder: Bool = false,
        dateFormat: HistoryMatchDateFormat = HistoryMatchDateFormat()
    ) {
        self.match = match
        self.presentation = presentation
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
        .accessibilityAddTraits(.isHeader)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        VStack(spacing: sectionSpacing) {
            statusLine
            if dynamicTypeSize.isAccessibilitySize {
                stackedMatchup
            } else {
                matchup
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: Status

    @ViewBuilder
    private var statusLine: some View {
        switch presentation {
        case .hidden:
            statusPill(String(localized: .matchBetsStartsLabel(dateTimeText)))
        case .betsClosed:
            statusPill(String(localized: .matchBetsClosedLabel))
        case .computed:
            Text(dateTimeText)
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
        }
    }

    private func statusPill(_ text: String) -> some View {
        Text(text)
            .font(.subheadline.weight(.semibold))
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
            .padding(.horizontal, pillHorizontalPadding)
            .padding(.vertical, pillVerticalPadding)
            .background(Capsule().fill(HistoryStyle.neutralFill))
    }

    // MARK: Teams and center

    /// Home on the left and away on the right of an intrinsic-width center; both team columns
    /// share the remaining width, which keeps the center in the middle.
    private var matchup: some View {
        HStack(alignment: .center, spacing: minimumGap) {
            teamColumn(code: match.homeTeamId, name: match.homeTeamName)
            center
            teamColumn(code: match.awayTeamId, name: match.awayTeamName)
        }
    }

    /// Accessibility text sizes leave no room for three columns, so the center sits between the
    /// home and away teams vertically.
    private var stackedMatchup: some View {
        VStack(alignment: .leading, spacing: sectionSpacing) {
            teamLine(code: match.homeTeamId, name: match.homeTeamName)
            center
                .frame(maxWidth: .infinity)
            teamLine(code: match.awayTeamId, name: match.awayTeamName)
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
            .font(.title3.weight(.semibold))
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
    }

    @ViewBuilder
    private var center: some View {
        switch MatchHeaderCenter(match: match, presentation: presentation) {
        case .versus:
            Text(.matchBetsVersusLabel)
                .font(.title2.weight(.heavy))
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
                .fixedSize()
        case .matchScore(let score):
            scoreView(home: score.homeTeamValue, away: score.awayTeamValue, size: matchScoreSize)
        case .finalScore(let score):
            scoreView(home: score?.homeTeamValue, away: score?.awayTeamValue, size: finalScoreSize)
        }
    }

    private func scoreView(home: Int?, away: Int?, size: CGFloat) -> some View {
        let isAvailable = home != nil && away != nil
        let digits = isAvailable ? HistoryStyle.primaryText : HistoryStyle.secondaryText
        // Centered, so the restrained separator sits midway up the digits.
        return HStack(alignment: .center, spacing: scoreSpacing) {
            Text(verbatim: home.map(String.init) ?? "—")
                .loadedForeground(digits, isPlaceholder: isPlaceholder)
            Text(verbatim: "–")
                .font(.system(size: size * 0.6, weight: .regular))
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
            Text(verbatim: away.map(String.init) ?? "—")
                .loadedForeground(digits, isPlaceholder: isPlaceholder)
        }
        .font(.system(size: size, weight: .bold))
        .monospacedDigit()
        .lineLimit(1)
        .fixedSize()
    }

    // MARK: Text

    private var dateTimeText: String {
        "\(dateFormat.date(match.matchDateTime)) · \(dateFormat.time(match.matchDateTime))"
    }

    /// The exact string handed to `.accessibilityLabel`; a placeholder contributes none.
    var accessibilityLabel: String {
        guard !isPlaceholder else { return "" }
        let home = match.homeTeamName
        let away = match.awayTeamName
        let matchup: String = switch MatchHeaderCenter(match: match, presentation: presentation) {
        case .versus:
            String(localized: .matchBetsMatchupAccessibility(home, away))
        case .matchScore(let score):
            String(localized: .historyMatchScoreAccessibility(home, score.homeTeamValue, away, score.awayTeamValue))
        case .finalScore(let score?):
            String(localized: .historyResultAccessibility(home, score.homeTeamValue, away, score.awayTeamValue))
        case .finalScore(nil):
            String(localized: .historyResultUnavailableAccessibility(home, away))
        }
        let status: String = switch presentation {
        case .hidden: String(localized: .matchBetsStartsLabel(dateTimeText))
        case .betsClosed: String(localized: .matchBetsClosedLabel)
        case .computed: dateTimeText
        }
        return "\(matchup). \(status)"
    }
}

/// What sits between the teams. An uncomputed score is only the score reported so far; a missing
/// computed result shows dashes, never an invented 0–0.
enum MatchHeaderCenter: Equatable {
    case versus
    case matchScore(TeamScore<Int>)
    case finalScore(TeamScore<Int>?)

    init(match: PoolGamblerBetModel, presentation: MatchBetsPresentation) {
        switch presentation {
        case .computed:
            self = .finalScore(match.matchScore)
        case .betsClosed:
            self = match.matchScore.map(MatchHeaderCenter.matchScore) ?? .versus
        case .hidden:
            self = .versus
        }
    }
}

/// Stable filler for the heading while the gate is unknown; never shown as real content.
func matchHeaderPlaceholderModel() -> PoolGamblerBetModel {
    poolGamblerBetPlaceholderModel(isLocked: false, isComputed: false).copy {
        $0.homeTeamName = String(repeating: "X", count: 9)
        $0.awayTeamName = String(repeating: "X", count: 9)
        $0.matchScore = nil
        $0.betScore = nil
        $0.score = nil
        $0.matchDateTime = Date(timeIntervalSince1970: 0)
    }
}

private let sectionSpacing: CGFloat = 16
private let minimumGap: CGFloat = 12
private let flagNameSpacing: CGFloat = 8
private let flagAspectRatio: CGFloat = 0.75
private let maximumFlagWidth: CGFloat = 72
private let scoreSpacing: CGFloat = 12
private let pillHorizontalPadding: CGFloat = 14
private let pillVerticalPadding: CGFloat = 6

#Preview("States") {
    ScrollView {
        VStack(spacing: 40) {
            ForEach(matchHeaderPreviewCases(), id: \.0) { _, match, presentation in
                MatchHeader(match: match, presentation: presentation)
                Divider()
            }
            MatchHeader(match: matchHeaderPlaceholderModel(), presentation: .hidden, isPlaceholder: true)
        }
        .historyHorizontalGutter()
    }
}

#Preview("Accessibility text") {
    ScrollView {
        VStack(spacing: 40) {
            ForEach(matchHeaderPreviewCases(), id: \.0) { _, match, presentation in
                MatchHeader(match: match, presentation: presentation)
            }
        }
        .historyHorizontalGutter()
    }
    .environment(\.dynamicTypeSize, .accessibility3)
}
