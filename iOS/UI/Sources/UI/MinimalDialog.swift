import SwiftUI
import UIKit

private struct MinimalDialogContainer<Base: View, DialogContent: View>: View {
    @Binding var isPresented: Bool
    let base: Base
    let dialogContent: () -> DialogContent

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.colorScheme) private var colorScheme

    private let cornerRadius: CGFloat = 16

    var body: some View {
        base
            .fullScreenCover(isPresented: $isPresented) {
                ZStack {
                    Color.black
                        .opacity(colorScheme == .dark ? 0.7 : 0.2)
                        .ignoresSafeArea()
                        .onTapGesture { isPresented = false }

                    dialogContent()
                        .background(
                            RoundedRectangle(cornerRadius: cornerRadius)
                                .fill(Color(.surfaceVariant))
                        )
                        .padding(.horizontal, boxSpacing.large)
                }
                .background(BackgroundClearView())
            }
    }
}

private struct BackgroundClearView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        DispatchQueue.main.async {
            view.superview?.superview?.backgroundColor = .clear
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}

public extension View {
    func minimalDialog<Content: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder content: @escaping () -> Content,
    ) -> some View {
        MinimalDialogContainer(isPresented: isPresented, base: self, dialogContent: content)
    }
}

#Preview {
    Color.blue
        .ignoresSafeArea()
        .minimalDialog(isPresented: .constant(true)) {
            VStack(spacing: 16) {
                Text("Dialog title").font(.title2)
                Text("Centered card over a dimmed backdrop.")
                Button("Close") {}
                    .buttonStyle(.borderedProminent)
            }
            .padding(24)
        }
}
