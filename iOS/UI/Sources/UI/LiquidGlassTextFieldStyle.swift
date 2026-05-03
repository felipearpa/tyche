import SwiftUI

public struct LiquidGlassTextFieldStyle: TextFieldStyle {
    public init() {}

    @ViewBuilder
    public func _body(configuration: TextField<Self._Label>) -> some View {
        if #available(iOS 26.0, *) {
            configuration
                .padding()
                .glassEffect(.regular, in: .capsule)
        } else {
            configuration
                .padding()
                .background(.ultraThinMaterial, in: .capsule)
        }
    }
}

public extension TextFieldStyle where Self == LiquidGlassTextFieldStyle {
    static var liquidGlass: LiquidGlassTextFieldStyle { LiquidGlassTextFieldStyle() }
}
