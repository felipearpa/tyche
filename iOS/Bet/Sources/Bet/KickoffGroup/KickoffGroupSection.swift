import SwiftUI
import UI

/// One kickoff group of History or Timeline: its header, then its match rows joined by a subdued
/// vertical rail and separated by thin dividers. Each row keeps its own match action and press
/// feedback; the header and rail are not actions. `owner` selects the rows' ownership wording.
///
/// Loading slots render this same component from `kickoffGroupPlaceholderModel()` with
/// `isPlaceholder: true`; the header and rows then render as placeholders and open nothing.
struct KickoffGroupSection: View {
    let group: KickoffGroupModel
    let owner: HistoryOwner
    let isPlaceholder: Bool
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        group: KickoffGroupModel,
        owner: HistoryOwner = .signedInGambler,
        isPlaceholder: Bool = false,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.group = group
        self.owner = owner
        self.isPlaceholder = isPlaceholder
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        VStack(alignment: .leading, spacing: headerSpacing) {
            KickoffGroupHeader(group: group, isPlaceholder: isPlaceholder)
                .historyHorizontalGutter()

            VStack(spacing: 0) {
                ForEach(Array(group.matches.enumerated()), id: \.element.id) { index, match in
                    if index > 0 {
                        Divider()
                    }
                    row(match)
                }
            }
            .padding(.leading, railWidth + railGap)
            .background(alignment: .leading) { rail }
            .padding(.leading, HistoryStyle.horizontalPadding)
            .padding(.trailing, HistoryStyle.horizontalPadding)
        }
        .padding(.bottom, boxSpacing.large)
    }

    @ViewBuilder
    private func row(_ match: PoolGamblerBetModel) -> some View {
        if isPlaceholder {
            HistoryBetItem(poolGamblerBet: match, isPlaceholder: true, owner: owner)
        } else {
            Button {
                invokeMatchOpen(onMatchOpen, match)
            } label: {
                HistoryBetItem(poolGamblerBet: match, owner: owner)
            }
            .buttonStyle(InteractiveRowButtonStyle())
        }
    }

    /// Decorative: it groups the rows visually and adds no focus stop.
    private var rail: some View {
        Capsule()
            .fill(HistoryStyle.railFill)
            .frame(width: railWidth)
            .padding(.vertical, railInset)
            .accessibilityHidden(true)
    }
}

/// The initial-load groups of History and Timeline: production group sections from placeholder
/// models.
struct KickoffGroupPlaceholderList: View {
    let count: Int

    var body: some View {
        ForEach(0..<count, id: \.self) { _ in
            KickoffGroupSection(group: kickoffGroupPlaceholderModel(), isPlaceholder: true)
        }
    }
}

private let headerSpacing: CGFloat = 4
private let railWidth: CGFloat = 2
private let railGap: CGFloat = 16
private let railInset: CGFloat = 8

#Preview("History, one to four matches") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(groupSizePreviewModels()) { group in
                KickoffGroupSection(group: group)
            }
        }
    }
}

#Preview("Timeline, pending and partial") {
    ScrollView {
        VStack(spacing: 0) {
            ForEach(timelineGroupPreviewModels().prefix(3)) { group in
                KickoffGroupSection(group: group, owner: .selectedGambler(name: "El mono"))
            }
        }
    }
}

#Preview("Placeholder") {
    ScrollView {
        VStack(spacing: 0) {
            KickoffGroupPlaceholderList(count: 2)
        }
    }
}
