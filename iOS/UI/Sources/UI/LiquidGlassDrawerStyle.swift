import SwiftUI

public struct LiquidGlassDrawerStyle: DrawerStyle {
    public init() {}

    @ViewBuilder
    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26.0, *) {
            let shape = UnevenRoundedRectangle(
                topLeadingRadius: 0,
                bottomLeadingRadius: 0,
                bottomTrailingRadius: 25,
                topTrailingRadius: 25
            )
            configuration.content
                .glassEffect(in: shape)
                .clipShape(shape)
        } else {
            DefaultDrawerStyle().makeBody(configuration: configuration)
        }
    }
}

public extension DrawerStyle where Self == LiquidGlassDrawerStyle {
    static var liquidGlass: LiquidGlassDrawerStyle { LiquidGlassDrawerStyle() }
}
