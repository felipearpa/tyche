import SwiftUI

public struct LiquidGlassTextFieldStyle: TextFieldStyle {
    public init() {}

    public func _body(configuration: TextField<Self._Label>) -> some View {
        Body(configuration: configuration)
    }

    private struct Body: View {
        @Environment(\.isEnabled) private var isEnabled
        let configuration: TextField<LiquidGlassTextFieldStyle._Label>

        @ViewBuilder
        var body: some View {
            if #available(iOS 26.0, *) {
                configuration
                    .padding()
                    .glassEffect(.regular, in: .capsule)
                    .opacity(isEnabled ? 1.0 : 0.5)
            } else {
                configuration
                    .padding()
                    .background(.ultraThinMaterial, in: .capsule)
            }
        }
    }
}

public extension TextFieldStyle where Self == LiquidGlassTextFieldStyle {
    static var liquidGlass: LiquidGlassTextFieldStyle { LiquidGlassTextFieldStyle() }
}
