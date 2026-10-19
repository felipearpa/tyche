import SwiftUI
import UI

/// A `+` or `−` button beside a team's score box: 52 × 44 pt, secondary-text glyph on the softer
/// stepper fill.
struct BetScoreStepper: View {
    enum Direction {
        case increase
        case decrease

        var delta: Int { self == .increase ? 1 : -1 }
    }

    let direction: Direction
    let teamName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(sharedResource: direction == .increase ? .add : .remove)
                .resizable()
                .frame(width: PendingBetStyle.stepperGlyphSize, height: PendingBetStyle.stepperGlyphSize)
                .foregroundStyle(PendingBetStyle.secondaryText)
                .frame(width: PendingBetStyle.stepperWidth, height: PendingBetStyle.stepperHeight)
                .background(
                    RoundedRectangle(cornerRadius: PendingBetStyle.scoreBoxCornerRadius)
                        .fill(PendingBetStyle.stepperFill)
                )
                .contentShape(RoundedRectangle(cornerRadius: PendingBetStyle.scoreBoxCornerRadius))
        }
        .buttonStyle(PendingBetPressStyle())
        .accessibilityLabel(Text(label))
    }

    private var label: LocalizedStringResource {
        switch direction {
        case .increase: .betIncreaseScoreAccessibility(teamName)
        case .decrease: .betDecreaseScoreAccessibility(teamName)
        }
    }
}

/// The failed save's banner: the kept-draft message and Retry. At accessibility text sizes Retry
/// moves below the message.
struct BetSaveFailureBanner: View {
    let onRetry: () -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: 8) {
                    message
                    retry
                }
            } else {
                HStack(spacing: 12) {
                    message
                    Spacer(minLength: 0)
                    retry
                }
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 6)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 12).fill(PendingBetStyle.errorFill))
    }

    private var message: some View {
        Text(.betSaveFailureText)
            .font(.subheadline)
            .foregroundStyle(PendingBetStyle.errorText)
            .fixedSize(horizontal: false, vertical: true)
    }

    private var retry: some View {
        Button(action: onRetry) {
            Text(sharedResource: .retryAction)
                .font(.body.weight(.semibold))
                .foregroundStyle(Color(sharedResource: .onError))
                .lineLimit(1)
                .fixedSize()
                .padding(.horizontal, 16)
                .frame(minHeight: 36)
                .background(Capsule().fill(Color(sharedResource: .error)))
                .frame(minWidth: PendingBetStyle.minimumTarget, minHeight: PendingBetStyle.minimumTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(PendingBetPressStyle())
    }
}

/// Immediate press feedback for the card's custom-drawn controls.
struct PendingBetPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.6 : 1)
    }
}

#Preview("Controls") {
    VStack(spacing: 16) {
        HStack {
            BetScoreStepper(direction: .increase, teamName: "Colombia", action: {})
            BetScoreStepper(direction: .decrease, teamName: "Colombia", action: {})
        }
        BetSaveFailureBanner(onRetry: {})
    }
    .padding()
    .background(PendingBetStyle.cardFill)
}
