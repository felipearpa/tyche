import SwiftUI

public struct ErrorView: View {
    private let localizedError: LocalizedError

    @Environment(\.boxSpacing) private var boxSpacing

    public init(localizedError: LocalizedError) {
        self.localizedError = localizedError
    }

    public var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(.sentimentDissatisfied)
                .resizable()
                .frame(width: iconSize, height: iconSize)

            VStack(spacing: boxSpacing.medium) {
                Text(localizedError.errorDescription ?? "")
                    .multilineTextAlignment(.center)
                    .font(.title)

                Text(localizedError.failureReason ?? "")
                    .multilineTextAlignment(.leading)

                Text(localizedError.recoverySuggestion ?? "")
                    .multilineTextAlignment(.leading)
            }
        }
    }
}

private let iconSize: CGFloat = 64

#Preview {
    ErrorView(localizedError: UnknownLocalizedError())
}
