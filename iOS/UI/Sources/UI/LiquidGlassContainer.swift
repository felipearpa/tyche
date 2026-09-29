import SwiftUI

/// Groups a local cluster of glass controls (for example a primary and a secondary action) so they
/// share one Liquid Glass rendering pass on iOS 26 and later.
///
/// Pass a single layout container, such as a `VStack` of buttons styled with `.liquidGlass` or
/// `.liquidGlassProminent`. On iOS 26 and later the content is placed in a `GlassEffectContainer`
/// whose blending spacing is zero, so controls separated by any gap stay distinct at rest and keep
/// their own hit areas. Below iOS 26 the same content is returned unchanged, with the same layout,
/// bindings, and actions.
///
/// Wrap only the adjacent glass controls, never a whole screen, a native toolbar or tab bar, or a
/// control supplied by a sign-in provider. The container adds no accessibility element.
public struct LiquidGlassContainer<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: 0) {
                content
            }
        } else {
            content
        }
    }
}

#Preview("Action pair") {
    VStack {
        Spacer()
        LiquidGlassContainer {
            VStack(spacing: 8) {
                Button(action: {}) {
                    Text(verbatim: "Primary action")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.liquidGlassProminent)

                Button(action: {}) {
                    Text(verbatim: "Secondary action")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.liquidGlass)
            }
        }
    }
    .padding()
}
