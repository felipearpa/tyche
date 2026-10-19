import SwiftUI
import UI

typealias PendingBetRowState = PendingBetRowHolder.RowState

extension PendingBetRowHolder.RowState {
    /// The status as the pill shows it and assistive technology reads it; Saving while the
    /// spinner stands in for the pill.
    var statusText: LocalizedStringResource {
        switch self {
        case .locked: .betStatusLocked
        case .empty: .betStatusNoBet
        case .incomplete: .betStatusIncomplete
        case .saving: .betSavingAccessibilityValue
        case .saved: .betStatusSaved
        case .failed: .betStatusNotSaved
        }
    }
}

/// The card's trailing status: one pill anchored to the trailing edge. No bet and Incomplete show
/// text, Saved, Not saved, and Locked an icon and text, and Saving the shared ball spinner.
///
/// A state change is one transaction on the shared curve: the pill's width eases from the old
/// content's width to the new one, its trailing edge fixed, while the old content fades out over
/// the first part of the change and the new content fades in after it, so they never overlap. The
/// width is measured from a hidden copy of the current content and applied as one animatable
/// frame, measured again when the text size changes. The height is fixed in every state. With Reduce Motion the content still crossfades but
/// the width changes at once.
///
/// Hidden from assistive technology: the card's summary element carries the status as its value.
struct PendingBetStatusSlot: View {
    let rowState: PendingBetRowState
    let isPlaceholder: Bool

    @ScaledMetric(relativeTo: .subheadline) private var slotHeight: CGFloat = 26
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var contentWidth: CGFloat?

    var body: some View {
        ZStack(alignment: .trailing) {
            PendingBetStatusContent(rowState: rowState, isPlaceholder: isPlaceholder, height: slotHeight)
                .fixedSize()
                .id(rowState)
                .transition(PendingBetStyle.contentSwap)
        }
        .frame(width: contentWidth, height: slotHeight, alignment: .trailing)
        .background {
            Capsule().fill(fill)
            Capsule().strokeBorder(rowState == .empty ? PendingBetStyle.emptyOutline : .clear, lineWidth: 1)
        }
        .clipShape(Capsule())
        .background(alignment: .trailing) { widthProbe }
        .animation(reduceMotion ? nil : PendingBetStyle.animation, value: rowState)
        .animation(reduceMotion ? nil : PendingBetStyle.animation, value: contentWidth)
        .onPreferenceChange(StatusContentWidthKey.self) { contentWidth = $0 }
        // A new text size invalidates the measured width: drop it, so the pill takes its natural
        // width at once and the probe measures it again.
        .onChange(of: dynamicTypeSize) { _ in contentWidth = nil }
        .accessibilityHidden(true)
    }

    /// The current content's natural width, measured without taking part in layout.
    private var widthProbe: some View {
        PendingBetStatusContent(rowState: rowState, isPlaceholder: isPlaceholder, height: slotHeight)
            .fixedSize()
            .hidden()
            .background(GeometryReader { proxy in
                Color.clear.preference(key: StatusContentWidthKey.self, value: proxy.size.width)
            })
    }

    private var fill: Color {
        switch rowState {
        case .locked, .saving: PendingBetStyle.controlFill
        case .empty: .clear
        case .incomplete: PendingBetStyle.amberFill
        case .saved: PendingBetStyle.savedFill
        case .failed: PendingBetStyle.errorFill
        }
    }
}

private struct StatusContentWidthKey: PreferenceKey {
    static let defaultValue: CGFloat? = nil

    static func reduce(value: inout CGFloat?, nextValue: () -> CGFloat?) {
        value = nextValue() ?? value
    }
}

/// What the pill holds in one state, with its own horizontal padding: the ball sits in a circle as
/// tall as the slot, text and icons in a capsule.
private struct PendingBetStatusContent: View {
    let rowState: PendingBetRowState
    let isPlaceholder: Bool
    let height: CGFloat

    @ScaledMetric(relativeTo: .subheadline) private var glyphSize: CGFloat = PendingBetStyle.pillGlyphSize

    var body: some View {
        if rowState == .saving && !isPlaceholder {
            BallSpinner()
                .frame(width: PendingBetStyle.spinnerSize, height: PendingBetStyle.spinnerSize)
                .padding(.horizontal, max(0, (height - PendingBetStyle.spinnerSize) / 2))
        } else {
            HStack(spacing: 4) {
                if let glyph, !isPlaceholder {
                    Image(sharedResource: glyph)
                        .resizable()
                        .frame(width: glyphSize, height: glyphSize)
                }
                Text(rowState.statusText)
                    .lineLimit(1)
                    .fixedSize()
            }
            .font(.subheadline.weight(.semibold))
            .loadedForeground(foreground, isPlaceholder: isPlaceholder)
            .padding(.horizontal, PendingBetStyle.pillHorizontalPadding)
        }
    }

    private var glyph: SharedImageResource? {
        switch rowState {
        case .locked: .lock
        case .saved: .done
        case .failed: .error
        case .empty, .incomplete, .saving: nil
        }
    }

    private var foreground: Color {
        switch rowState {
        case .locked, .empty, .saving: PendingBetStyle.secondaryText
        case .incomplete: PendingBetStyle.amber
        case .saved: PendingBetStyle.savedText
        case .failed: PendingBetStyle.errorText
        }
    }
}

#Preview("Status slot") {
    VStack(alignment: .trailing, spacing: 12) {
        ForEach([PendingBetRowState.empty, .incomplete, .saving, .saved, .failed, .locked], id: \.self) { state in
            PendingBetStatusSlot(rowState: state, isPlaceholder: false)
        }
    }
    .padding()
    .background(PendingBetStyle.cardFill)
}

/// Steps through a save so the pill's crossfade and width change can be watched.
private struct PendingBetStatusSlotCyclePreview: View {
    @State private var rowState = PendingBetRowState.saved

    var body: some View {
        VStack(alignment: .trailing, spacing: 24) {
            PendingBetStatusSlot(rowState: rowState, isPlaceholder: false)
            Button("Next state".excludeLocalize) {
                let order: [PendingBetRowState] = [.empty, .incomplete, .saving, .saved, .failed, .locked]
                rowState = order[(order.firstIndex(of: rowState)! + 1) % order.count]
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .trailing)
        .background(PendingBetStyle.cardFill)
    }
}

#Preview("Status slot, state changes") {
    PendingBetStatusSlotCyclePreview()
}
