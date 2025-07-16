import SwiftUI

private struct DrawerView<ContentView: View>: View {
    @Binding var isShowing: Bool
    var content: () -> ContentView
    var edgeTransition: AnyTransition = .move(edge: .leading)

    init(isShowing: Binding<Bool>, @ViewBuilder content: @escaping () -> ContentView) {
        self._isShowing = isShowing
        self.content = content
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment:.leading) {
                if (isShowing) {
                    Color.black
                        .opacity(0.3)
                        .onTapGesture {
                            isShowing.toggle()
                        }

                    ZStack {
                        content()
                            .padding(.top, geometry.safeAreaInsets.top)
                            .padding(.bottom, geometry.safeAreaInsets.bottom)
                    }
                    .frame(width: UIScreen.main.bounds.width * 0.9)
                    .background(Color(.systemBackground))
                    .clipShape(
                        .rect(
                            topLeadingRadius: 0,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 25,
                            topTrailingRadius: 25
                        )
                    )
                    .transition(edgeTransition)
                }
            }
            .ignoresSafeArea()
            .animation(.easeInOut, value: isShowing)
        }
    }
}

public extension View {
    func drawer<Content: View>(isShowing: Binding<Bool>, @ViewBuilder content: @escaping () -> Content) -> some View {
        self.overlay(
            DrawerView(isShowing: isShowing, content: content)
        )
    }
}

#Preview {
    DrawerView(isShowing: .constant(true)) {
        Text("Content")
    }
}
