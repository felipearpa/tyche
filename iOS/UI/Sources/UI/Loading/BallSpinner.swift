import SwiftUI

public struct BallSpinner: View {
    @State var degreesRotating = 0.0

    public init() {}

    public var body: some View {
        Image(.tycheLogo)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .foregroundStyle(Color.accentColor)
            .rotationEffect(.degrees(degreesRotating))
            .onAppear {
                withAnimation(
                    .linear(duration: 2)
                    .repeatForever(autoreverses: false)) {
                        degreesRotating = 360.0
                    }
            }
    }
}

#Preview {
    BallSpinner()
        .frame(width: 64, height: 64)
}
