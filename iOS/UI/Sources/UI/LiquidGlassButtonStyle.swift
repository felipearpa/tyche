import SwiftUI

public struct LiquidGlassButtonStyle: PrimitiveButtonStyle {
    public init() {}

    @ViewBuilder
    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            Button(configuration).buttonStyle(.glass)
        } else {
            Button(configuration).buttonStyle(.bordered)
        }
    }
}

public struct LiquidGlassProminentButtonStyle: PrimitiveButtonStyle {
    public init() {}

    @ViewBuilder
    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            Button(configuration).buttonStyle(.glassProminent)
        } else {
            Button(configuration).buttonStyle(.borderedProminent)
        }
    }
}

public extension PrimitiveButtonStyle where Self == LiquidGlassButtonStyle {
    static var liquidGlass: LiquidGlassButtonStyle { LiquidGlassButtonStyle() }
}

public extension PrimitiveButtonStyle where Self == LiquidGlassProminentButtonStyle {
    static var liquidGlassProminent: LiquidGlassProminentButtonStyle { LiquidGlassProminentButtonStyle() }
}
