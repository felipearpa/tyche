import SwiftUI
import UI
import Session
import ViewingState

public struct SocialSignInRow: View {
    let googleState: LoadState<AccountBundle>
    let onSignInWithGoogle: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        googleState: LoadState<AccountBundle>,
        onSignInWithGoogle: @escaping () -> Void
    ) {
        self.googleState = googleState
        self.onSignInWithGoogle = onSignInWithGoogle
    }

    private var isDisabled: Bool {
        googleState.isLoading() || googleState.isLoaded()
    }

    public var body: some View {
        HStack(spacing: boxSpacing.medium) {
            Button(action: onSignInWithGoogle) {
                Image(.googleLogo)
                    .resizable()
                    .renderingMode(.original)
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize)
            }
            .frame(width: buttonSize, height: buttonSize)
//            .background(Color(sharedResource: .secondaryContainer), in: Circle())
            .disabled(isDisabled)
        }
    }
}

private let buttonSize: CGFloat = 56
private let iconSize: CGFloat = 28

#Preview("Idle") {
    SocialSignInRow(googleState: .idle, onSignInWithGoogle: {})
}

#Preview("Loading") {
    SocialSignInRow(googleState: .loading, onSignInWithGoogle: {})
}

#Preview("Loaded") {
    SocialSignInRow(
        googleState: .loaded(AccountBundle(
            accountId: "preview-account",
            externalAccountId: "preview-external",
            email: "preview@example.com"
        )),
        onSignInWithGoogle: {}
    )
}

#Preview("Failure") {
    SocialSignInRow(googleState: .failure(UnknownLocalizedError()), onSignInWithGoogle: {})
}
