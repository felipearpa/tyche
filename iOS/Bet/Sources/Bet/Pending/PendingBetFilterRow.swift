import SwiftUI
import UI

/// The All / No bet / Placed chips with their counts. The selected chip takes the app's selection
/// pair (`primaryContainer` with `onPrimaryContainter` text, as the pool creator's selected
/// layout; white on `#1B5E20` is 7.9:1, and 4.8:1 for the count at 70 %); the others keep their
/// outline. The list changes the filter on the shared curve, so the
/// fill and text colors interpolate, or change at once with Reduce Motion. VoiceOver
/// reads each chip's name, its match count ("at least N" while more pages may load, when the
/// count shows a trailing "+"), and whether it is selected. When the chips do not fit
/// (large text), the row scrolls horizontally.
struct PendingBetFilterRow: View {
    let selected: PendingBetFilter
    let counts: PendingBetFilterCounts
    /// More pages may still load: each count shows a trailing "+" and is read as "at least N".
    let isPartial: Bool
    let onSelect: (PendingBetFilter) -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ViewThatFits(in: .horizontal) {
            chips
            ScrollView(.horizontal, showsIndicators: false) {
                chips
            }
        }
        // The "+" leaving with the last page changes the chips' widths on the shared curve.
        .animation(reduceMotion ? nil : PendingBetStyle.animation, value: isPartial)
        .animation(reduceMotion ? nil : PendingBetStyle.animation, value: counts)
    }

    private var chips: some View {
        HStack(spacing: 8) {
            ForEach(PendingBetFilter.allCases, id: \.self) { filter in
                PendingBetFilterChip(
                    title: filter.title,
                    count: counts.count(for: filter),
                    isPartial: isPartial,
                    isSelected: filter == selected,
                    action: { onSelect(filter) }
                )
            }
        }
    }
}

private struct PendingBetFilterChip: View {
    let title: LocalizedStringResource
    let count: Int
    let isPartial: Bool
    let isSelected: Bool
    let action: () -> Void

    /// The count in the locale's number format, exact or with the partial "+": "3,000", "1,123+".
    private var countLabel: String {
        isPartial ? String(localized: .betsFilterCountPartial(count)) : count.formatted(.number)
    }

    private var countText: some View {
        Text(verbatim: countLabel)
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(title)
                    .font(.body.weight(.semibold))
                countText
                    .hidden()
                    // The hidden copy sizes the chip to the new count at once, so its width eases
                    // on the row's animation; the visible count swaps without overlap.
                    .overlay(alignment: .leading) {
                        ZStack(alignment: .leading) {
                            countText
                                .fixedSize()
                                .id(countLabel)
                                .transition(PendingBetStyle.contentSwap)
                        }
                    }
                    .font(.body)
                    .monospacedDigit()
                    .opacity(0.7)
            }
            .lineLimit(1)
            .fixedSize()
            .foregroundStyle(isSelected ? Color(sharedResource: .onPrimaryContainter) : PendingBetStyle.primaryText)
            .padding(.horizontal, 16)
            .frame(minHeight: PendingBetStyle.chipHeight)
            .background {
                Capsule().fill(isSelected ? Color(sharedResource: .primaryContainer) : .clear)
                Capsule().strokeBorder(isSelected ? .clear : PendingBetStyle.emptyOutline, lineWidth: 1)
            }
            .frame(minHeight: PendingBetStyle.minimumTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(PendingBetPressStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(title))
        .accessibilityValue(Text(isPartial ? .betsFilterCountAtLeastAccessibility(count) : .historyGroupMatchCount(count)))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

extension PendingBetFilter {
    var title: LocalizedStringResource {
        switch self {
        case .all: .betsFilterAll
        case .noBet: .betsFilterNoBet
        case .placed: .betsFilterPlaced
        }
    }

    /// The message shown when the filter keeps no loaded match. All keeps every match, so it has
    /// none: an empty pool shows the list's own empty state.
    var emptyMessage: LocalizedStringResource? {
        switch self {
        case .all: nil
        case .noBet: .betsFilterEmptyMessage
        case .placed: .betsFilterPlacedEmptyMessage
        }
    }
}

extension PendingBetFilterCounts {
    func count(for filter: PendingBetFilter) -> Int {
        switch filter {
        case .all: all
        case .noBet: noBet
        case .placed: placed
        }
    }
}

#Preview("Filter row") {
    VStack(alignment: .leading, spacing: 16) {
        PendingBetFilterRow(selected: .all, counts: PendingBetFilterCounts(all: 6, noBet: 3, placed: 3), isPartial: false, onSelect: { _ in })
        PendingBetFilterRow(selected: .noBet, counts: PendingBetFilterCounts(all: 6, noBet: 2, placed: 4), isPartial: true, onSelect: { _ in })
    }
    .padding()
}
