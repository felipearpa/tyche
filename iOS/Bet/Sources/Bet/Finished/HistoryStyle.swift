import SwiftUI
import UI

/// Colors and measurements shared by History's summary and rows.
///
/// Measured contrast (WCAG): `secondaryText` is 5.9:1 on the light canvas and 7.7:1 on the dark
/// one, and 5.6:1 / 6.4:1 on `neutralFill`. `positiveText` is 6.6:1 on the light canvas and
/// 12.5:1 on the dark one, and 5.5:1 / 7.3:1 on `positiveFill`.
enum HistoryStyle {
    static let secondaryText = Color(sharedResource: .onSurface).opacity(0.7)
    static let primaryText = Color(sharedResource: .onSurface)
    static let positiveText = Color(sharedResource: .currentUser)
    static let positiveFill = Color(sharedResource: .currentUserContainer)
    static let neutralFill = Color(uiColor: .secondarySystemFill)

    static let horizontalPadding: CGFloat = 16
}

extension View {
    /// History's horizontal gutter. Rows span the full content width on every window size, like
    /// the Scores and Bets lists; the team columns stay balanced around the centered result.
    func historyHorizontalGutter() -> some View {
        padding(.horizontal, HistoryStyle.horizontalPadding)
    }
}

/// The visible points text: a signed value for positive awards, a plain value for zero or
/// negative ones, and a neutral dash when the value is unavailable. It never shows a bare plus.
func historyPointsShortText(_ points: HistoryPoints) -> String {
    switch points {
    case .earned(let value) where value > 0:
        return String(localized: .historyPositivePointsShort(value))
    case .earned(let value):
        return String(localized: .historyPointsShort(value))
    case .unavailable:
        return String(localized: .historyPointsUnavailableShort)
    }
}

extension HistoryPoints {
    var isPositive: Bool {
        if case .earned(let value) = self { return value > 0 }
        return false
    }
}
