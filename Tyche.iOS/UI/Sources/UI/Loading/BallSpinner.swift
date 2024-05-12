import SwiftUI

private let ICON_SIZE: CGFloat = 64

struct BallSpinner: View {
    @State var degreesRotating = 0.0
    
    var body: some View {
        Image(.tycheLogo)
            .resizable()
            .frame(width: ICON_SIZE, height: ICON_SIZE)
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
}
