import SwiftUI

/// Primary inline action filled with the app accent: `.borderedProminent` on every OS version, at
/// the inherited control size, with the on-accent label color.
///
/// Use it for accent-filled actions in forms and list content. An action with another fill, such
/// as an error-tinted retry, keeps `.borderedProminent` with its own tint and label.
public struct StandardProminentButtonStyle: PrimitiveButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        Button(accentFilled: configuration).buttonStyle(.borderedProminent)
    }
}

public extension PrimitiveButtonStyle where Self == StandardProminentButtonStyle {
    static var standardProminent: StandardProminentButtonStyle { StandardProminentButtonStyle() }
}

extension Button where Label == AccentFilledLabel {
    /// Rebuilds a styled button with its label drawn in `onPrimary` while enabled. The button
    /// keeps its role, action, and identity; only the label's color changes with the enabled
    /// state.
    init(accentFilled configuration: PrimitiveButtonStyleConfiguration) {
        self.init(role: configuration.role, action: configuration.trigger) {
            AccentFilledLabel(label: configuration.label)
        }
    }
}

/// The label of an accent-filled control. While enabled it uses `onPrimary`: white on the light
/// accent (#2E7D32, 5.13:1) and black on the dark accent (#4CAF50, 7.56:1), where the system's
/// white label measures 2.78:1. While disabled it sets no color, so the control keeps the system's
/// disabled label on its disabled fill.
struct AccentFilledLabel: View {
    let label: PrimitiveButtonStyleConfiguration.Label
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        if isEnabled {
            label.foregroundStyle(Color(sharedResource: .onPrimary))
        } else {
            label
        }
    }
}
