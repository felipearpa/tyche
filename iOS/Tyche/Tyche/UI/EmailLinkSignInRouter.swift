import SwiftUI
import Session
import Account

struct EmailLinkSignInRouter: View {
    let email: String
    let emailLink: String
    let onSignIn: (AccountBundle) -> Void

    @Environment(\.diResolver) private var diResolver

    var body: some View {
        NavigationStack {
            EmailLinkSignInView(
                viewModel: EmailLinkSignInViewModel(
                    signInWithEmailLinkUseCase: diResolver.resolve(SignInWithEmailLinkUseCase.self)!
                ),
                email: email,
                emailLink: emailLink,
                onStart: onSignIn,
            )
        }
    }
}
