import SwiftUI

public extension Color {
    /// Fortuna brand accent (amber/yellow, `#FFC107`) — matches the Android `secondary` color.
    /// Used to highlight owner-only management affordances (e.g. the active "Done" edit toggle and
    /// the "Owner tools" drawer section header).
    static let brandAccent = Color(
        .sRGB,
        red: 1.0,
        green: 193.0 / 255.0,
        blue: 7.0 / 255.0,
        opacity: 1.0
    )
}
