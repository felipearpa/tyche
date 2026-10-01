import SwiftUI

// Both glass styles use the large control size: standalone actions need a 44-point touch target,
// and the large size stays at least 47 points tall at every Dynamic Type size (the regular size is
// 34 points at the default text size).

/// Secondary standalone action: `.glass` on iOS 26 and later, `.bordered` below, at the large
/// control size.
///
/// The `.bordered` fallback draws its label in the tint color, and the accent measures below
/// 4.5:1 on the gray bordered fill, so the fallback uses the primary color, as the glass label
/// does. This tint is applied inside the style, so a tint set by the caller does not reach the
/// fallback.
public struct LiquidGlassButtonStyle: PrimitiveButtonStyle {
    public init() {}

    @ViewBuilder
    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            Button(configuration).buttonStyle(.glass).controlSize(.large)
        } else {
            Button(configuration).buttonStyle(.bordered).controlSize(.large).tint(.primary)
        }
    }
}

/// Primary standalone action filled with the app accent: `.glassProminent` on iOS 26 and later,
/// `.borderedProminent` below, at the large control size, with the on-accent label color.
public struct LiquidGlassProminentButtonStyle: PrimitiveButtonStyle {
    public init() {}

    @ViewBuilder
    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            Button(accentFilled: configuration).buttonStyle(.glassProminent).controlSize(.large)
        } else {
            Button(accentFilled: configuration).buttonStyle(.borderedProminent).controlSize(.large)
        }
    }
}

public extension PrimitiveButtonStyle where Self == LiquidGlassButtonStyle {
    static var liquidGlass: LiquidGlassButtonStyle { LiquidGlassButtonStyle() }
}

public extension PrimitiveButtonStyle where Self == LiquidGlassProminentButtonStyle {
    static var liquidGlassProminent: LiquidGlassProminentButtonStyle { LiquidGlassProminentButtonStyle() }
}
