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

    var body: some View {
        ZStack {
            if let reservedText {
                Text(reservedText).hidden()
            }
            Text(text)
        }
        .font(.body.weight(.semibold))
        .monospacedDigit()
        .lineLimit(1)
        .fixedSize()
        .loadedForeground(foreground, isPlaceholder: isPlaceholder)
        .padding(.horizontal, pillHorizontalPadding)
        .padding(.vertical, pillVerticalPadding)
        .background(Capsule().fill(fill))
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
