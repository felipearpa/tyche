import Core
import SwiftUI
import UI

/// The scrolling header of one kickoff group in History and Timeline: the localized date, then
/// the time with the group's total match count, and the group's movement badge at the trailing
/// edge. A partial group's computed count sits under the badge. The header is not an action and
/// is announced once, as a heading, with the date and time, the total count, and the movement.
/// A one-match group omits the visible count.
///
/// Loading slots render this same component from `kickoffGroupPlaceholderModel()` with
/// `isPlaceholder: true`: it conceals its content with the shared pulse and stays out of the
/// accessibility tree.
struct KickoffGroupHeader: View {
    let group: KickoffGroupModel
    let isPlaceholder: Bool
    let dateFormat: HistoryMatchDateFormat

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    init(
        group: KickoffGroupModel,
        isPlaceholder: Bool = false,
        dateFormat: HistoryMatchDateFormat = HistoryMatchDateFormat()
    ) {
        self.group = group
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(.isHeader)
        .accessibilityHidden(isPlaceholder)
    }

    /// Metadata and badge share a row; at accessibility text sizes, or when a long date leaves no
    /// room, the badge moves below the metadata instead of clipping either.
    @ViewBuilder
    private var content: some View {
        if dynamicTypeSize.isAccessibilitySize {
            stackedContent
        } else {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: minimumGap) {
                    metadata
                    Spacer(minLength: 0)
                    badge
                }
                stackedContent
            }
        }
    }

    private var stackedContent: some View {
        VStack(alignment: .leading, spacing: stackedSpacing) {
            metadata
            badge
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var metadata: some View {
        VStack(alignment: .leading, spacing: metadataLineSpacing) {
            Text(dateText)
                .font(.headline)
                .loadedForeground(HistoryStyle.primaryText, isPlaceholder: isPlaceholder)
            Text(timeAndCountText)
                .font(.subheadline)
                .loadedForeground(HistoryStyle.secondaryText, isPlaceholder: isPlaceholder)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private var badge: some View {
        TrendIndicator(
            movement: group.movement,
            textStyle: .body,
            style: .badge,
            isPlaceholder: isPlaceholder
        )
    }

    // MARK: Text

    private var dateText: String {
        dateFormat.date(group.kickoffDateTime)
    }

    private var timeText: String {
        dateFormat.time(group.kickoffDateTime)
    }

    private var matchCountText: String {
        String(localized: .historyGroupMatchCount(group.totalMatches))
    }

    private var timeAndCountText: String {
        group.totalMatches > 1 ? "\(timeText) · \(matchCountText)" : timeText
    }

    /// The exact string handed to `.accessibilityLabel`. A placeholder contributes none.
    var accessibilityLabel: String {
        guard !isPlaceholder else { return "" }
        return [
            "\(dateText), \(timeText)",
            matchCountText,
            group.movement.accessibilityText,
        ]
        .joined(separator: ". ")
    }
}

private let minimumGap: CGFloat = 12
private let metadataLineSpacing: CGFloat = 2
private let stackedSpacing: CGFloat = 8

#Preview("States") {
    ScrollView {
        VStack(alignment: .leading, spacing: 24) {
            ForEach(timelineGroupPreviewModels()) { group in
                KickoffGroupHeader(group: group)
            }
            KickoffGroupHeader(group: kickoffGroupPlaceholderModel(), isPlaceholder: true)
        }
        .historyHorizontalGutter()
    }
}

#Preview("Accessibility text") {
    ScrollView {
        VStack(alignment: .leading, spacing: 24) {
            ForEach(timelineGroupPreviewModels()) { group in
                KickoffGroupHeader(group: group)
            }
        }
        .historyHorizontalGutter()
    }
    .environment(\.dynamicTypeSize, .accessibility3)
}
