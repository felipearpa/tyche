import SwiftUI
import UI

/// The points the signed-in gambler has earned in the current pool: a large value and a smaller
/// label. The initial load renders this same component from `historyPointsPlaceholderModel` with
/// `isPlaceholder: true`; it then conceals its content with the shared pulse and stays out of the
/// accessibility tree.
struct HistoryPointsSummary: View {
    let points: HistoryPoints
    let isPlaceholder: Bool

    @ScaledMetric(relativeTo: .largeTitle) private var valueSize: CGFloat = 52

    init(points: HistoryPoints, isPlaceholder: Bool = false) {
        self.points = points
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
        switch points {
        case .earned(let value):
            return String(localized: .historySummaryPointsAccessibility(value))
        case .unavailable:
            return String(localized: .historySummaryUnavailableAccessibility)
        }
    }
}

/// Stable filler for the initial summary placeholder; never shown as real content.
let historyPointsPlaceholderModel = HistoryPoints.earned(100)

private let labelSpacing: CGFloat = 8

/// History's summary for every state of the total: the placeholder while the first request runs,
/// the confirmed points (with refresh progress or a refresh failure beneath them), or a compact
/// error with a retry that requests only the total.
struct HistoryPointsHeader: View {
    let state: HistoryPointsSummaryState
    /// Pull to refresh shows its own indicator, so the inline progress appears only for a summary
    /// retry.
    let showsRefreshProgress: Bool
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.medium) {
            switch state.presentation {
            case .placeholder:
                HistoryPointsSummary(points: historyPointsPlaceholderModel, isPlaceholder: true)
            case .failed:
                failure(.historySummaryLoadFailure)
            case .points(let points, let refreshStatus):
                HistoryPointsSummary(points: points)
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
            failure(.historySummaryRefreshFailure)
        }
    }

    private func failure(_ message: LocalizedStringResource) -> some View {
        VStack(alignment: .leading, spacing: boxSpacing.medium) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(HistoryStyle.secondaryText)
                .fixedSize(horizontal: false, vertical: true)

            Button(action: onRetry) {
                Text(sharedResource: .retryAction)
            }
            .buttonStyle(.standardProminent)
        }
    }
}

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
    }
    .padding()
}
