import SwiftUI
import UI

/// The points the owner has earned in the current pool: a large value and a smaller label. The
/// initial load renders this same component from `historyPointsPlaceholderModel` with
/// `isPlaceholder: true`; it then conceals its content with the shared pulse and stays out of the
/// accessibility tree. Another gambler's total is announced with that gambler's name.
struct HistoryPointsSummary: View {
    let points: HistoryPoints
    let owner: HistoryOwner
    let isPlaceholder: Bool

    @ScaledMetric(relativeTo: .largeTitle) private var valueSize: CGFloat = 52

    init(points: HistoryPoints, owner: HistoryOwner = .signedInGambler, isPlaceholder: Bool = false) {
        self.points = points
        self.owner = owner
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        // The label follows the value on one baseline and moves below it when they no longer
        // fit, as at accessibility text sizes.
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: labelSpacing) {
                value
                label
            }
            VStack(alignment: .leading, spacing: 0) {
                value
                label
            }
        }
    }

    private var value: some View {
        Text(historyPointsShortText(points))
            .font(.system(size: valueSize, weight: .bold))
            .monospacedDigit()
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(
                points.isPositive ? HistoryStyle.positiveText : HistoryStyle.primaryText,
                isPlaceholder: isPlaceholder
            )
    }

    private var label: some View {
        Text(points == .unavailable ? .historyTotalUnavailableLabel : .historyEarnedLabel)
            .font(.title3)
            .fixedSize(horizontal: false, vertical: true)
            .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
    }

    /// The exact string handed to `.accessibilityLabel`; a placeholder contributes none.
    var accessibilityLabel: String {
        guard !isPlaceholder else { return "" }
        switch (owner, points) {
        case (.signedInGambler, .earned(let value)):
            return String(localized: .historySummaryPointsAccessibility(value))
        case (.signedInGambler, .unavailable):
            return String(localized: .historySummaryUnavailableAccessibility)
        case (.selectedGambler(let name), .earned(let value)):
            return String(localized: .historyGamblerSummaryPointsAccessibility(name, points: value))
        case (.selectedGambler(let name), .unavailable):
            return String(localized: .historyGamblerSummaryUnavailableAccessibility(name))
        }
    }
}

/// Stable filler for the initial summary placeholder; never shown as real content.
let historyPointsPlaceholderModel = HistoryPoints.earned(100)

private let labelSpacing: CGFloat = 8

/// The points summary for every state of the total: the placeholder while the first request
/// runs, the confirmed points (with refresh progress or a refresh failure beneath them), or a
/// compact error with a retry that requests only the total. Shared by History and Timeline;
/// `owner` selects the ownership wording.
struct HistoryPointsHeader: View {
    let state: HistoryPointsSummaryState
    let owner: HistoryOwner
    /// Pull to refresh shows its own indicator, so the inline progress appears only for a summary
    /// retry.
    let showsRefreshProgress: Bool
    let onRetry: () -> Void

    init(
        state: HistoryPointsSummaryState,
        owner: HistoryOwner = .signedInGambler,
        showsRefreshProgress: Bool,
        onRetry: @escaping () -> Void
    ) {
        self.state = state
        self.owner = owner
        self.showsRefreshProgress = showsRefreshProgress
        self.onRetry = onRetry
    }

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.medium) {
            switch state.presentation {
            case .placeholder:
                HistoryPointsSummary(points: historyPointsPlaceholderModel, owner: owner, isPlaceholder: true)
            case .failed:
                failure(loadFailureMessage)
            case .points(let points, let refreshStatus):
                HistoryPointsSummary(points: points, owner: owner)
                refreshStatusView(refreshStatus)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func refreshStatusView(_ status: HistoryPointsSummaryState.RefreshStatus) -> some View {
        switch status {
        case .current:
            EmptyView()
        case .refreshing where showsRefreshProgress:
            HStack(spacing: boxSpacing.medium) {
                ProgressView()
                Text(.historySummaryRefreshing)
                    .font(.subheadline)
                    .foregroundStyle(HistoryStyle.secondaryText)
            }
            .accessibilityElement(children: .combine)
        case .refreshing:
            EmptyView()
        case .failed:
            failure(refreshFailureMessage)
        }
    }

    private var loadFailureMessage: LocalizedStringResource {
        switch owner {
        case .signedInGambler: .historySummaryLoadFailure
        case .selectedGambler: .historyGamblerSummaryLoadFailure
        }
    }

    private var refreshFailureMessage: LocalizedStringResource {
        switch owner {
        case .signedInGambler: .historySummaryRefreshFailure
        case .selectedGambler: .historyGamblerSummaryRefreshFailure
        }
    }

    private func failure(_ message: LocalizedStringResource) -> some View {
        HStack(spacing: boxSpacing.small) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(HistoryStyle.secondaryText)
                .fixedSize(horizontal: false, vertical: true)

            HistoryPointsRetryButton(action: onRetry)
        }
    }
}

/// The summary's retry: a secondary, icon-only action beside the failure text, so it does not
/// compete with the list's own Retry. It requests only the total. The glyph scales with Dynamic
/// Type and the whole 44-point minimum area takes taps.
struct HistoryPointsRetryButton: View {
    let action: () -> Void

    @ScaledMetric(relativeTo: .subheadline) private var glyphSize: CGFloat = 22

    var body: some View {
        Button(action: action) {
            Image(sharedResource: .refresh)
                .resizable()
                .frame(width: glyphSize, height: glyphSize)
                .frame(minWidth: minimumHitTarget, minHeight: minimumHitTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(Text(sharedResource: .retryAction))
    }
}

private let minimumHitTarget: CGFloat = 44

#Preview("Positive, zero, unavailable") {
    VStack(alignment: .leading, spacing: 24) {
        HistoryPointsSummary(points: .earned(120))
        HistoryPointsSummary(points: .earned(1))
        HistoryPointsSummary(points: .earned(0))
        HistoryPointsSummary(points: .unavailable)
        HistoryPointsSummary(points: historyPointsPlaceholderModel, isPlaceholder: true)
    }
    .padding()
}

#Preview("States") {
    VStack(alignment: .leading, spacing: 24) {
        HistoryPointsHeader(state: .initial.loading(), showsRefreshProgress: false, onRetry: {})
        HistoryPointsHeader(state: .initial.failed(), showsRefreshProgress: false, onRetry: {})
        HistoryPointsHeader(
            state: .initial.loaded(.earned(4)).loading(),
            showsRefreshProgress: true,
            onRetry: {}
        )
        HistoryPointsHeader(state: .initial.loaded(.earned(4)).failed(), showsRefreshProgress: false, onRetry: {})
        HistoryPointsHeader(
            state: .initial.failed(),
            owner: .selectedGambler(name: "El mono"),
            showsRefreshProgress: false,
            onRetry: {}
        )
    }
    .padding()
}
