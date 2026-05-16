import SwiftUI

private struct DrawerContainer<Base: View, DrawerContent: View>: View {
    @Binding var isShowing: Bool
    let base: Base
    let drawerContent: () -> DrawerContent

    @Environment(\.drawerStyle) private var style

    private let drawerWidthRatio: CGFloat = 0.85
    private let dragThreshold: CGFloat = 50
    private let cornerRadius: CGFloat = 25
    private let openBlurRadius: CGFloat = 4

    var body: some View {
        GeometryReader { geometry in
            let fullWidth = geometry.size.width
                + geometry.safeAreaInsets.leading
                + geometry.safeAreaInsets.trailing
            let drawerWidth = fullWidth * drawerWidthRatio

            ZStack(alignment: .leading) {
                style.makeBody(configuration: DrawerStyleConfiguration(
                    content: DrawerStyleConfiguration.Content(view: AnyView(
                        drawerContent()
                            .padding(.top, geometry.safeAreaInsets.top)
                            .padding(.bottom, geometry.safeAreaInsets.bottom)
                    ))
                ))
                .frame(width: drawerWidth)
                .frame(maxHeight: .infinity)

                base
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .blur(radius: isShowing ? openBlurRadius : 0)
                    .compositingGroup()
                    .clipShape(
                        .rect(
                            topLeadingRadius: isShowing ? cornerRadius : 0,
                            bottomLeadingRadius: isShowing ? cornerRadius : 0,
                            bottomTrailingRadius: 0,
                            topTrailingRadius: 0,
                        )
                    )
                    .shadow(color: .black.opacity(isShowing ? 0.3 : 0), radius: 10, x: -3)
                    .offset(x: isShowing ? drawerWidth : 0)
                    .disabled(isShowing)
                    .overlay(
                        Color.black
                            .opacity(isShowing ? 0.25 : 0)
                            .onTapGesture { isShowing = false }
                            .allowsHitTesting(isShowing)
                    )
            }
            .ignoresSafeArea()
            .animation(.easeOut(duration: 0.3), value: isShowing)
            .gesture(
                DragGesture()
                    .onEnded { value in
                        if value.translation.width < -dragThreshold {
                            isShowing = false
                        } else if value.translation.width > dragThreshold {
                            isShowing = true
                        }
                    }
            )
        }
    }
}

public extension View {
    func drawer<Content: View>(
        isShowing: Binding<Bool>,
        @ViewBuilder content: @escaping () -> Content,
    ) -> some View {
        DrawerContainer(isShowing: isShowing, base: self, drawerContent: content)
    }
}

#Preview {
    Color.blue
        .ignoresSafeArea()
        .drawer(isShowing: .constant(true)) {
            Text("Content")
        }
}
