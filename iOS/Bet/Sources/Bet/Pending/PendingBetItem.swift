import SwiftUI
import UI
import Core

/// What an open card's controls do. Locked and placeholder cards have none.
struct PendingBetItemActions {
    let setHome: (Int?) -> Void
    let setAway: (Int?) -> Void
    let step: (PendingBetRowHolder.Team, Int) -> Void
    let retry: () -> Void
    /// Focus entered one of the score boxes from outside the card.
    let focusEnter: () -> Void
    /// Focus left both score boxes.
    let focusLeave: () -> Void
}

/// One pending match as a card: kickoff time, the "Locks in N min" note during the last hour
/// before the lock, the status slot, both teams' flags and names, and the score area.
///
/// - Open cards (`actions` given, any state but `.locked`) show `+`/`−` steppers around two
///   typeable score boxes; typing the first digit into the home box moves focus to the away box.
///   An `.incomplete` card marks the missing box in amber and its box tells VoiceOver whose score
///   is needed; `.failed` adds the banner with Retry, the only content that changes an open card's
///   height.
/// - Locked cards show the scores read-only in muted boxes, with no controls.
/// - The status slot, boxes, and steppers keep their sizes in every state. The failure banner
///   enters and leaves with a height-plus-opacity transition (opacity only with Reduce Motion, and
///   no animated height), as does a card changing to locked.
///
/// The kickoff line and status form the card's summary element for VoiceOver: the matchup, time,
/// and lock note as its label and the status, Saving included, as its value.
///
/// Loading slots render this same component from a placeholder model with `isPlaceholder: true`:
/// it takes the locked layout, so it has no controls, conceals its content with the shared pulse,
/// ignores touches, and stays out of the accessibility tree.
struct PendingBetItem: View {
    let match: PoolGamblerBetModel
    let draft: PendingBetRowHolder.Draft
    let rowState: PendingBetRowState
    let actions: PendingBetItemActions?
    let isPlaceholder: Bool

    @FocusState private var focusedTeam: PendingBetRowHolder.Team?
    /// The height of the card's content, which the card's own height eases toward.
    @State private var contentHeight: CGFloat?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    init(
        match: PoolGamblerBetModel,
        draft: PendingBetRowHolder.Draft,
        rowState: PendingBetRowState,
        actions: PendingBetItemActions?,
        isPlaceholder: Bool = false
    ) {
        self.match = match
        self.draft = draft
        self.rowState = rowState
        self.actions = actions
        self.isPlaceholder = isPlaceholder
    }

    private var editActions: PendingBetItemActions? {
        guard !isPlaceholder, rowState != .locked else { return nil }
        return actions
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { card(lockCountdown: nil) }
            } else {
                TimelineView(.explicit(lockCountdownChanges(kickoff: match.matchDateTime, after: .now))) { context in
                    card(lockCountdown: lockCountdownMinutes(kickoff: match.matchDateTime, now: context.date))
                }
            }
        }
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
        // Moving between the two boxes keeps focus on the card; only leaving both counts.
        .onChange(of: focusedTeam) { [focusedTeam] team in
            if team == nil {
                actions?.focusLeave()
            } else if focusedTeam == nil {
                actions?.focusEnter()
            }
        }
    }

    private func card(lockCountdown: Int?) -> some View {
        VStack(spacing: 12) {
            header(lockCountdown: lockCountdown)
            teamsAndScores
            belowScores
        }
        .padding(.horizontal, PendingBetStyle.cardHorizontalPadding)
        .padding(.vertical, PendingBetStyle.cardVerticalPadding)
        .frame(maxWidth: .infinity)
        .fixedSize(horizontal: false, vertical: true)
        .onGeometryChange(for: CGFloat.self, of: { proxy in proxy.size.height }) { height in
            contentHeightChanged(height)
        }
        .modifier(AnimatedCardHeight(height: contentHeight))
        .background {
            let shape = RoundedRectangle(cornerRadius: PendingBetStyle.cardCornerRadius)
            shape.fill(PendingBetStyle.cardFill)
            if lockCountdown != nil {
                shape.strokeBorder(PendingBetStyle.amber, lineWidth: 1)
            }
        }
        // Rows leaving below the scores stay inside the card while its height shrinks.
        .clipShape(RoundedRectangle(cornerRadius: PendingBetStyle.cardCornerRadius))
        // One transaction per state change. With Reduce Motion the card's height changes at once;
        // the status slot and score boxes still fade through their own animations.
        .animation(reduceMotion ? nil : PendingBetStyle.animation, value: rowState)
    }

    /// The card's height follows its content on the shared curve, interpolated frame by frame, so
    /// the list lays out the cards below at every step and they move with the card instead of
    /// jumping to its final height. The first measurement and Reduce Motion apply at once.
    private func contentHeightChanged(_ height: CGFloat) {
        guard contentHeight != nil, !reduceMotion else {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { contentHeight = height }
            return
        }
        withAnimation(PendingBetStyle.animation) { contentHeight = height }
    }

    /// At accessibility text sizes the status moves below the kickoff line.
    private func header(lockCountdown: Int?) -> some View {
        let layout = dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8))
            : AnyLayout(HStackLayout(spacing: 8))
        return layout {
            kickoffLine(lockCountdown: lockCountdown)
                .frame(maxWidth: .infinity, alignment: .leading)
            PendingBetStatusSlot(rowState: rowState, isPlaceholder: isPlaceholder)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(summaryLabel(lockCountdown: lockCountdown)))
        .accessibilityValue(Text(rowState.statusText))
    }

    private func kickoffLine(lockCountdown: Int?) -> some View {
        let layout = dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 4))
            : AnyLayout(HStackLayout(alignment: .firstTextBaseline, spacing: 8))
        return layout {
            Text(verbatim: match.matchDateTime.toShortTimeString())
                .font(.subheadline)
                .loadedForeground(PendingBetStyle.secondaryText, isPlaceholder: isPlaceholder)
            if let lockCountdown {
                Text(.betLocksInMinutes(lockCountdown))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(PendingBetStyle.amber)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func summaryLabel(lockCountdown: Int?) -> String {
        let matchup = String(localized: .matchBetsMatchupAccessibility(match.homeTeamName, match.awayTeamName))
        let parts = [matchup, match.matchDateTime.toShortTimeString()]
            + [lockCountdown.map { String(localized: .betLocksInMinutes($0)) }].compactMap { $0 }
        return parts.joined(separator: ", ")
    }

    /// Teams flank the scores; at accessibility text sizes each team takes its own line above
    /// the scores, so names wrap between words instead of inside them.
    @ViewBuilder
    private var teamsAndScores: some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: 8) {
                PendingBetTeam(teamId: match.homeTeamId, teamName: match.homeTeamName, layout: .row, isPlaceholder: isPlaceholder)
                PendingBetTeam(teamId: match.awayTeamId, teamName: match.awayTeamName, layout: .row, isPlaceholder: isPlaceholder)
                scores
                    .frame(maxWidth: .infinity)
            }
        } else {
            HStack(alignment: .center, spacing: 8) {
                PendingBetTeam(teamId: match.homeTeamId, teamName: match.homeTeamName, layout: .column, isPlaceholder: isPlaceholder)
                scores
                PendingBetTeam(teamId: match.awayTeamId, teamName: match.awayTeamName, layout: .column, isPlaceholder: isPlaceholder)
            }
        }
    }

    private var scores: some View {
        HStack(spacing: 6) {
            scoreColumn(.home)
            Text(verbatim: ":")
                .font(.title3.weight(.bold))
                .loadedForeground(PendingBetStyle.secondaryText, isPlaceholder: isPlaceholder)
                .accessibilityHidden(true)
            scoreColumn(.away)
        }
        .fixedSize()
    }

    @ViewBuilder
    private func scoreColumn(_ team: PendingBetRowHolder.Team) -> some View {
        let value = team == .home ? draft.home : draft.away
        let otherValue = team == .home ? draft.away : draft.home
        let teamName = team == .home ? match.homeTeamName : match.awayTeamName

        if let editActions {
            VStack(spacing: 6) {
                BetScoreStepper(direction: .increase, teamName: teamName) { editActions.step(team, 1) }
                BetScoreBox(
                    value: value,
                    otherValue: otherValue,
                    teamName: teamName,
                    isFocused: focusedTeam == team,
                    onInput: { typed in input(typed, for: team, actions: editActions) },
                    onOverflow: team == .home ? { rest in overflowFromHome(rest, actions: editActions) } : nil
                )
                .focused($focusedTeam, equals: team)
                BetScoreStepper(direction: .decrease, teamName: teamName) { editActions.step(team, -1) }
            }
        } else {
            BetScoreReadOnlyBox(value: value, teamName: teamName)
        }
    }

    /// A digit typed into the home box moves focus on to the away box.
    private func input(_ value: Int?, for team: PendingBetRowHolder.Team, actions: PendingBetItemActions) {
        switch team {
        case .home:
            actions.setHome(value)
            if value != nil, focusedTeam == .home { focusedTeam = .away }
        case .away:
            actions.setAway(value)
        }
    }

    /// Digits typed into the home box after its first one belong to the away box.
    private func overflowFromHome(_ rest: Int, actions: PendingBetItemActions) {
        actions.setAway(rest)
        focusedTeam = .away
    }

    @ViewBuilder
    private var belowScores: some View {
        if let editActions {
            if rowState == .failed {
                BetSaveFailureBanner(onRetry: editActions.retry)
                    .transition(belowScoresTransition)
            }
        }
    }

    private var belowScoresTransition: AnyTransition {
        reduceMotion ? .opacity : .move(edge: .top).combined(with: .opacity)
    }
}

/// A team's flag and name: the flag over the name beside the scores, or the flag before the name
/// on its own line. The summary element and the score controls name the team, so it is hidden
/// from VoiceOver.
private struct PendingBetTeam: View {
    enum Layout {
        case column
        case row
    }

    let teamId: String
    let teamName: String
    let layout: Layout
    let isPlaceholder: Bool

    var body: some View {
        Group {
            switch layout {
            case .column:
                VStack(spacing: 6) {
                    flag
                    name.multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
            case .row:
                HStack(spacing: 12) {
                    flag
                    name
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .accessibilityHidden(true)
    }

    private var flag: some View {
        FlagImage(teamCode: teamId)
            .frame(width: PendingBetStyle.flagSize, height: PendingBetStyle.flagSize)
    }

    private var name: some View {
        Text(verbatim: teamName)
            .font(.body.weight(.medium))
            .loadedForeground(PendingBetStyle.primaryText, isPlaceholder: isPlaceholder)
            .lineLimit(3)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// Whole minutes until predictions lock, ten minutes before kickoff, rounded up; nil unless the
/// lock is in the next sixty minutes. The count only changes at whole minutes before the lock, so
/// it never shows a minute that has already passed, and it reaches nil at the lock itself.
func lockCountdownMinutes(kickoff: Date, now: Date) -> Int? {
    let remaining = lockTime(kickoff: kickoff).timeIntervalSince(now)
    guard remaining > 0, remaining <= lockNoticeWindow else { return nil }
    return Int((remaining / 60).rounded(.up))
}

/// The card's lock-note timeline: `now`, so the first render shows the current count, then each
/// later instant at which `lockCountdownMinutes` changes, the whole minutes before the lock during
/// the last hour and the lock itself, when the note disappears. Only `now` once the lock has
/// passed, so a locked card's timeline stops.
func lockCountdownChanges(kickoff: Date, after now: Date) -> [Date] {
    let lock = lockTime(kickoff: kickoff)
    let minutes = Int(lockNoticeWindow / 60)
    let changes = (0...minutes)
        .map { lock.addingTimeInterval(-Double($0) * 60) }
        .filter { $0 > now }
        .reversed()
    return [now] + changes
}

private func lockTime(kickoff: Date) -> Date {
    kickoff.addingTimeInterval(-lockLeadTime)
}

private let lockLeadTime: TimeInterval = 10 * 60
private let lockNoticeWindow: TimeInterval = 60 * 60

/// The scores a placeholder card is redacted over, as wide as any real score.
let placeholderDraft: PendingBetRowHolder.Draft = (home: 88, away: 88)

/// Every card state, for previews.
struct PendingBetItemStatesPreview: View {
    private let match = poolGamblerBetDummyModel().copy {
        $0.isComputed = false
        $0.homeTeamName = "Colombia"
        $0.awayTeamName = "Portugal"
        $0.homeTeamId = "co"
        $0.awayTeamId = "pt"
        $0.matchDateTime = Date().addingTimeInterval(3 * 3600)
    }
    private let actions = PendingBetItemActions(
        setHome: { _ in }, setAway: { _ in }, step: { _, _ in }, retry: {}, focusEnter: {}, focusLeave: {}
    )

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                PendingBetItem(match: match, draft: (nil, nil), rowState: .empty, actions: actions)
                PendingBetItem(match: match, draft: (2, nil), rowState: .incomplete, actions: actions)
                PendingBetItem(match: match, draft: (2, 1), rowState: .saving, actions: actions)
                PendingBetItem(match: match, draft: (2, 1), rowState: .saved, actions: actions)
                PendingBetItem(match: match, draft: (2, 1), rowState: .failed, actions: actions)
                PendingBetItem(
                    match: match.copy { $0.matchDateTime = Date().addingTimeInterval(18 * 60) },
                    draft: (nil, nil),
                    rowState: .empty,
                    actions: actions
                )
                PendingBetItem(match: match.copy { $0.lock() }, draft: (2, 0), rowState: .locked, actions: nil)
                PendingBetItem(
                    match: poolGamblerBetPlaceholderModel(),
                    draft: placeholderDraft,
                    rowState: .locked,
                    actions: nil,
                    isPlaceholder: true
                )
            }
            .padding(16)
        }
    }
}

#Preview("States, dark") {
    PendingBetItemStatesPreview()
        .preferredColorScheme(.dark)
}

#Preview("States, light") {
    PendingBetItemStatesPreview()
        .preferredColorScheme(.light)
}

#Preview("States, large text") {
    PendingBetItemStatesPreview()
        .environment(\.dynamicTypeSize, .accessibility3)
}

#Preview("States, Spanish") {
    PendingBetItemStatesPreview()
        .environment(\.locale, Locale(identifier: "es-CO"))
}

/// Gives the card an explicit height that SwiftUI interpolates on every frame of an animation, so
/// the stack holding the card lays it out, and the cards after it, at each step. Without a height
/// the card takes its content's.
private struct AnimatedCardHeight: ViewModifier, Animatable {
    var height: CGFloat?

    var animatableData: CGFloat {
        get { height ?? 0 }
        set { height = newValue }
    }

    func body(content: Content) -> some View {
        content.frame(height: height, alignment: .top)
    }
}
