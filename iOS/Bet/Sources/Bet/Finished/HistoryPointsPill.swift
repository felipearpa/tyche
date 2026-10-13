import SwiftUI
import UI

/// The capsule that shows a points value or status in History, Timeline, and the match screen's
/// participant rows. A positive award uses the positive colors; everything else is neutral.
/// Inside the signed-in gambler's highlighted row the colors switch to the current-user roles, so
/// the pill stays distinct from the row's own fill.
struct HistoryPointsPill: View {
    enum Surface {
        /// The inherited screen canvas.
        case canvas
        /// The signed-in gambler's `currentUserContainer` row.
        case currentUserRow
    }

    let text: String
    let isPositive: Bool
    let surface: Surface
    /// When set, the pill is at least as wide as this text, so a column of pills lines up.
    let reservedText: String?
    let isPlaceholder: Bool

    init(
        text: String,
        isPositive: Bool,
        surface: Surface = .canvas,
        reservedText: String? = nil,
        isPlaceholder: Bool = false
    ) {
        self.text = text
        self.isPositive = isPositive
        self.surface = surface
        self.reservedText = reservedText
        self.isPlaceholder = isPlaceholder
    }

    /// One line when it fits; otherwise, at large text on a narrow width, the text wraps between
    /// words instead of pushing the pill past the row's edge.
    var body: some View {
        ViewThatFits(in: .horizontal) {
            ZStack {
                if let reservedText {
                    Text(reservedText).hidden()
                }
                Text(text)
            }
            .lineLimit(1)
            .fixedSize()
            Text(text)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .font(.body.weight(.semibold))
        .monospacedDigit()
        .loadedForeground(foreground, isPlaceholder: isPlaceholder)
        .padding(.horizontal, pillHorizontalPadding)
        .padding(.vertical, pillVerticalPadding)
        .background(Capsule().fill(fill))
        // Neighboring text gives way first, so the pill keeps one line wherever it did before.
        .layoutPriority(1)
    }

    private var foreground: Color {
        switch (surface, isPositive) {
        case (.canvas, true): HistoryStyle.positiveText
        case (.canvas, false): HistoryStyle.secondaryText
        case (.currentUserRow, true): Color(sharedResource: .onCurrentUser)
        case (.currentUserRow, false): Color(sharedResource: .onCurrentUserContainer)
        }
    }

    private var fill: Color {
        switch (surface, isPositive) {
        case (.canvas, true): HistoryStyle.positiveFill
        case (.canvas, false): HistoryStyle.neutralFill
        case (.currentUserRow, true): Color(sharedResource: .currentUser)
        case (.currentUserRow, false): HistoryStyle.neutralFill
        }
    }
}

private let pillHorizontalPadding: CGFloat = 14
private let pillVerticalPadding: CGFloat = 6
