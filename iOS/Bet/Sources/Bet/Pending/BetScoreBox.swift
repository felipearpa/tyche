import SwiftUI
import UIKit
import UI

/// How a score box looks, from its value, its neighbor's value, and focus.
enum BetScoreBoxAppearance: Equatable {
    /// No value and no value on the other side either: a dashed outline around "?".
    case empty
    /// No value while the other side has one: an amber dashed outline around an amber "?".
    case missing
    /// A value: a filled box.
    case filled
    /// The box being typed into: a green border.
    case focused
    /// A locked card's read-only score in muted text.
    case locked

    init(value: Int?, otherValue: Int?, isFocused: Bool, isLocked: Bool) {
        if isLocked {
            self = .locked
        } else if isFocused {
            self = .focused
        } else if value != nil {
            self = .filled
        } else {
            self = otherValue == nil ? .empty : .missing
        }
    }
}

/// The digits a score box keeps from typed text: ASCII digits only, leading zeros removed, at most
/// two digits. Empty text is no score (nil), never 0.
func parseBetScoreInput(_ text: String) -> Int? {
    let digits = text.filter { $0.isASCII && $0.isNumber }
    guard !digits.isEmpty else { return nil }
    let significant = digits.drop { $0 == "0" }
    guard !significant.isEmpty else { return 0 }
    return Int(significant.prefix(maximumScoreDigits))
}

private let maximumScoreDigits = 2

/// Splits typed text for a box that hands off after one digit: the first typed digit stays, and
/// any digits after it, typed faster than focus could move on, belong to the next box.
func splitFirstTypedDigit(_ text: String) -> (first: Int?, rest: Int?) {
    let digits = text.filter { $0.isASCII && $0.isNumber }
    guard let first = digits.first else { return (nil, nil) }
    return (Int(String(first)), parseBetScoreInput(String(digits.dropFirst())))
}

/// A team's editable score: a 52-pt box that opens the number pad. Focusing it selects its value,
/// so a typed digit replaces it. `onInput` receives each typed value after `parseBetScoreInput`;
/// values the card sets by other means (steppers, a restore) only redraw the box.
struct BetScoreBox: View {
    let value: Int?
    let otherValue: Int?
    let teamName: String
    let isFocused: Bool
    let onInput: (Int?) -> Void
    /// Set on a box that hands focus on after its first digit: digits typed after that one reach
    /// this instead of the box, so typing quickly, or on a hardware keyboard, before focus has
    /// moved never puts two digits here.
    let onOverflow: ((Int) -> Void)?

    @State private var text: String

    init(
        value: Int?,
        otherValue: Int?,
        teamName: String,
        isFocused: Bool,
        onInput: @escaping (Int?) -> Void,
        onOverflow: ((Int) -> Void)? = nil
    ) {
        self.value = value
        self.otherValue = otherValue
        self.teamName = teamName
        self.isFocused = isFocused
        self.onInput = onInput
        self.onOverflow = onOverflow
        self._text = State(initialValue: value.map(String.init) ?? "")
    }

    private var appearance: BetScoreBoxAppearance {
        BetScoreBoxAppearance(value: value, otherValue: otherValue, isFocused: isFocused, isLocked: false)
    }

    var body: some View {
        TextField("".excludeLocalize, text: $text)
            .keyboardType(.numberPad)
            .multilineTextAlignment(.center)
            .font(PendingBetStyle.scoreFont)
            .foregroundStyle(PendingBetStyle.primaryText)
            .tint(PendingBetStyle.focusedOutline)
            .frame(width: PendingBetStyle.scoreBoxSize, height: PendingBetStyle.scoreBoxSize)
            .background {
                BetScoreBoxFrame(appearance: appearance)
            }
            .overlay {
                if value == nil && !isFocused {
                    BetScoreQuestionMark(appearance: appearance)
                        .allowsHitTesting(false)
                }
            }
            .contentShape(RoundedRectangle(cornerRadius: PendingBetStyle.scoreBoxCornerRadius))
            .accessibilityLabel(Text(.betScoreBoxAccessibility(teamName)))
            .accessibilityValue(boxAccessibilityValue)
            .onChange(of: text, perform: textChanged)
            .onChange(of: value) { newValue in
                let shown = newValue.map(String.init) ?? ""
                if text != shown { text = shown }
            }
            .onChange(of: isFocused) { focused in
                if focused { selectFocusedText() }
            }
    }

    /// The score, or for the missing side of an incomplete card, that this team's score is needed
    /// to save; the card shows no hint line, so the box says it.
    private var boxAccessibilityValue: Text {
        appearance == .missing ? Text(.betIncompleteHint(teamName)) : scoreAccessibilityValue(value)
    }

    /// Normalizes what was typed and reports a changed value. A redraw from a new `value` leaves
    /// the text equal to that value, so it reports nothing.
    private func textChanged(_ newText: String) {
        if let onOverflow, parseBetScoreInput(newText) != value {
            let split = splitFirstTypedDigit(newText)
            if let rest = split.rest {
                text = split.first.map(String.init) ?? ""
                if split.first != value { onInput(split.first) }
                onOverflow(rest)
                return
            }
        }
        let parsed = parseBetScoreInput(newText)
        let normalized = parsed.map(String.init) ?? ""
        if normalized != newText { text = normalized }
        if parsed != value { onInput(parsed) }
    }
}

/// A locked or placeholder card's score: the same box geometry with no input.
struct BetScoreReadOnlyBox: View {
    let value: Int?
    let teamName: String

    var body: some View {
        Text(verbatim: value.map(String.init) ?? "–")
            .font(PendingBetStyle.scoreFont)
            .foregroundStyle(PendingBetStyle.secondaryText)
            .frame(width: PendingBetStyle.scoreBoxSize, height: PendingBetStyle.scoreBoxSize)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(.betScoreBoxAccessibility(teamName)))
            .accessibilityValue(scoreAccessibilityValue(value))
    }
}

private func scoreAccessibilityValue(_ value: Int?) -> Text {
    value.map { Text(verbatim: String($0)) } ?? Text(.betScoreBoxEmptyAccessibilityValue)
}

/// The box's fill and outline. Only colors change between appearances, never the size.
private struct BetScoreBoxFrame: View {
    let appearance: BetScoreBoxAppearance

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: PendingBetStyle.scoreBoxCornerRadius)
        ZStack {
            shape.fill(fill)
            shape.strokeBorder(stroke, style: strokeStyle)
        }
        .animation(PendingBetStyle.animation, value: appearance)
        .accessibilityHidden(true)
    }

    private var fill: Color {
        switch appearance {
        case .empty, .locked: .clear
        case .missing: PendingBetStyle.amberFill
        case .filled, .focused: PendingBetStyle.controlFill
        }
    }

    private var stroke: Color {
        switch appearance {
        case .empty: PendingBetStyle.emptyOutline
        case .missing: PendingBetStyle.amber
        case .focused: PendingBetStyle.focusedOutline
        case .filled, .locked: .clear
        }
    }

    private var strokeStyle: StrokeStyle {
        switch appearance {
        case .empty, .missing: StrokeStyle(lineWidth: 1.5, dash: [4, 3])
        case .focused: StrokeStyle(lineWidth: 2)
        case .filled, .locked: StrokeStyle(lineWidth: 0)
        }
    }
}

private struct BetScoreQuestionMark: View {
    let appearance: BetScoreBoxAppearance

    var body: some View {
        Text(verbatim: "?")
            .font(PendingBetStyle.scoreFont)
            .foregroundStyle(appearance == .missing ? PendingBetStyle.amber : PendingBetStyle.secondaryText)
            .animation(PendingBetStyle.animation, value: appearance)
            .accessibilityHidden(true)
    }
}

/// Selects all text of the field that just became first responder, so the next digit replaces
/// it. SwiftUI's `TextField` has no selection API before iOS 18; the field is reached through the
/// responder chain on the next run-loop pass, once it holds focus. Setting the range directly,
/// unlike `selectAll(_:)`, shows no edit menu.
@MainActor
private func selectFocusedText() {
    DispatchQueue.main.async {
        guard let field = FirstResponderLookup.find() as? UITextField else { return }
        field.selectedTextRange = field.textRange(from: field.beginningOfDocument, to: field.endOfDocument)
    }
}

@MainActor
private enum FirstResponderLookup {
    static weak var found: UIResponder?

    static func find() -> UIResponder? {
        found = nil
        UIApplication.shared.sendAction(#selector(UIResponder.reportAsFirstResponder), to: nil, from: nil, for: nil)
        return found
    }
}

private extension UIResponder {
    @objc func reportAsFirstResponder() {
        MainActor.assumeIsolated { FirstResponderLookup.found = self }
    }
}

#Preview("Appearances") {
    HStack(spacing: 12) {
        BetScoreBox(value: nil, otherValue: nil, teamName: "Colombia", isFocused: false, onInput: { _ in })
        BetScoreBox(value: nil, otherValue: 2, teamName: "Colombia", isFocused: false, onInput: { _ in })
        BetScoreBox(value: 2, otherValue: 1, teamName: "Colombia", isFocused: false, onInput: { _ in })
        BetScoreBox(value: 2, otherValue: 1, teamName: "Colombia", isFocused: true, onInput: { _ in })
        BetScoreReadOnlyBox(value: 2, teamName: "Colombia")
    }
    .padding()
    .background(PendingBetStyle.cardFill)
}
