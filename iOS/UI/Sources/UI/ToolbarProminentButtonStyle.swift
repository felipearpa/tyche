import SwiftUI

/// Accent-filled circular toolbar action, such as create pool: a 44-point circle whose whole area
/// takes taps, with the glyph in the on-accent color.
///
/// On iOS 26 and later the style draws its own accent-tinted interactive glass circle. The native
/// prominent toolbar item only takes taps on a 32 × 36-point content area inside its 44-point glass,
/// so this is the one app-drawn glass control in a bar. Below iOS 26 it is a circle filled with the
/// inherited tint (the app accent).
///
/// Put the button in a `PlainToolbarItem`, which hides the shared toolbar background behind the
/// circle. Make the button's label the glyph image sized by `glyphSize`, and give the button an
/// accessibility label: an iOS 26 toolbar reduces a `Label` to its icon at the icon's intrinsic size.
public struct ToolbarProminentButtonStyle: PrimitiveButtonStyle {
    /// Diameter of the accent circle; the hit area covers the whole circle.
    public static let diameter: CGFloat = 44
    /// Side of the icon inside the circle.
    public static let glyphSize: CGFloat = 28

    public init() {}

    @ViewBuilder
    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            Button(role: configuration.role, action: configuration.trigger) {
                AccentFilledLabel(label: configuration.label)
                    .frame(width: Self.diameter, height: Self.diameter)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            // Glass takes a concrete tint color, so this branch uses the app accent directly.
            .glassEffect(.regular.tint(Color.accentColor).interactive(), in: Circle())
        } else {
            Button(accentFilled: configuration)
                .buttonStyle(AccentCircleButtonStyle())
        }
    }
}

public extension PrimitiveButtonStyle where Self == ToolbarProminentButtonStyle {
    static var toolbarProminent: ToolbarProminentButtonStyle { ToolbarProminentButtonStyle() }
}

/// The pre-iOS 26 circle: a tint fill that dims while pressed, and a gray fill while disabled.
private struct AccentCircleButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .frame(width: ToolbarProminentButtonStyle.diameter, height: ToolbarProminentButtonStyle.diameter)
            .background {
                if isEnabled {
                    Circle().fill(.tint)
                } else {
                    Circle().fill(Color(uiColor: .tertiarySystemFill))
                }
            }
            .contentShape(Circle())
            .opacity(configuration.isPressed ? 0.7 : 1)
    }
}
