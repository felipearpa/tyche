import SwiftUI

public struct IndeterminateLinearProgressView: View {
    @State private var x: CGFloat = -1
    var height: CGFloat = 4

    public var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.accentColor.opacity(0.2))
                    .frame(height: height)
                Capsule()
                    .fill(LinearGradient(
                        colors: [.clear, .primary.opacity(0.35), .clear],
                        startPoint: .leading, endPoint: .trailing))
                    .frame(width: geo.size.width * 0.35, height: height)
                    .offset(x: x * geo.size.width)
                    .onAppear {
                        withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                            x = 1.2
                        }
                    }
            }
        }
        .frame(height: height)
    }
}

#Preview {
    VStack {
        IndeterminateLinearProgressView()
            .padding()
    }
}

