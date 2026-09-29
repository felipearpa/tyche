import SwiftUI
import UIKit

/// Measurements shared by the standard input style and by layouts that size a field from its
/// content, such as prediction score fields.
public enum InputMetrics {
    /// Space between the entered text and the field boundary on every edge.
    public static let contentPadding: CGFloat = 16

    /// Width of the field boundary, drawn inside the padded area.
    public static let borderWidth: CGFloat = 1
}

/// Theme-aware, non-glass text-field appearance for content-layer inputs (forms, list rows).
///
/// Liquid Glass belongs to navigation and standalone actions, so this style uses semantic colors
/// on every OS version. An enabled field is filled with `tertiarySystemFill`, the system fill for
/// input fields. It is translucent, so it always reads as a step darker (light) or lighter (dark)
/// than whatever it sits on, plain or grouped, base or elevated. Its boundary meets 3:1 against the
/// surrounding background. A disabled field keeps its geometry and its text at full label contrast
/// (so a draft stays readable while it is being saved) but drops the fill and uses a quiet boundary,
/// and the system reports it as dimmed to assistive technologies.
///
/// A tap anywhere inside the capsule, including the padding, focuses the field.
public struct StandardTextFieldStyle: TextFieldStyle {
    public init() {}

    public func _body(configuration: TextField<Self._Label>) -> some View {
        Body(configuration: configuration)
    }

    private struct Body: View {
        @Environment(\.isEnabled) private var isEnabled
        @FocusState private var isFocused: Bool
        let configuration: TextField<StandardTextFieldStyle._Label>

        var body: some View {
            configuration
                // An additional focus binding on the same field: the consumer's own `focused`
                // binding keeps ownership of initial focus, and both report the same state.
                .focused($isFocused)
                .padding(InputMetrics.contentPadding)
                .background {
                    // Sits behind the field, so it receives only taps on the padding: the text
                    // line keeps the system's caret placement and selection gestures.
                    Capsule()
                        .fill(isEnabled ? StandardInputColors.fill : .clear)
                        .contentShape(Capsule())
                        .onTapGesture { isFocused = true }
                        .accessibilityHidden(true)
                }
                .overlay {
                    Capsule()
                        .strokeBorder(
                            isEnabled ? StandardInputColors.border : StandardInputColors.disabledBorder,
                            lineWidth: InputMetrics.borderWidth
                        )
                        .allowsHitTesting(false)
                }
        }
    }
}

/// Semantic colors of the standard input. Exposed to the module so contrast can be tested.
enum StandardInputColors {
    /// Translucent, so the field separates from plain and grouped screen backgrounds alike.
    static let fillUIColor = UIColor.tertiarySystemFill
    /// `systemGray` falls just short of 3:1 on light grouped backgrounds, so the light appearance
    /// always uses its increased-contrast variant; the dark appearance follows the system setting.
    static let borderUIColor = UIColor { traits in
        let lightTraits = UITraitCollection(traitsFrom: [
            traits,
            UITraitCollection(accessibilityContrast: .high),
        ])
        return UIColor.systemGray.resolvedColor(
            with: traits.userInterfaceStyle == .dark ? traits : lightTraits
        )
    }
    static let disabledBorderUIColor = UIColor.separator

    static let fill = Color(uiColor: fillUIColor)
    static let border = Color(uiColor: borderUIColor)
    static let disabledBorder = Color(uiColor: disabledBorderUIColor)
}

public extension TextFieldStyle where Self == StandardTextFieldStyle {
    static var standard: StandardTextFieldStyle { StandardTextFieldStyle() }
}

#Preview("Enabled and disabled") {
    VStack(spacing: 16) {
        TextField("Email", text: .constant(""))
        TextField("Email", text: .constant("gambler@example.com"))
        TextField("Email", text: .constant("gambler@example.com"))
            .disabled(true)
    }
    .textFieldStyle(.standard)
    .padding()
}
