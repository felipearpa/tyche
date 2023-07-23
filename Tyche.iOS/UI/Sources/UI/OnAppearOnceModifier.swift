import SwiftUI

public extension View {
    func onAppearOnce(_ perform: @escaping () -> ()) -> some View {
        self.modifier(OnAppearOnceModifier(perform: perform))
    }
}
private struct OnAppearOnceModifier: ViewModifier {
    let perform: () -> ()
    @State private var appearOnce = true
    func body(content: Content) -> some View {
        content
            .onAppear {
                if appearOnce {
                    appearOnce = false
                    perform()
                }
            }
    }
}
