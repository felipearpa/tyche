import SwiftUI
import Session
import Account

struct EmailLinkSignInRouter: View {
    let email: String
    let emailLink: String

    @Environment(\.diResolver) private var diResolver;
    @State private var signedInAccountBundle: AccountBundle? = nil
    
    var body: some View {
        if signedInAccountBundle == nil {
            NavigationStack {
                EmailLinkSignInView(
                    viewModel: EmailLinkSignInViewModel(
                        signInWithEmailLinkUseCase: diResolver.resolve(SignInWithEmailLinkUseCase.self)!
                    ),
                    email: email,
                    emailLink: emailLink,
                    onStart: { accountBundle in
                        signedInAccountBundle = accountBundle
                    }
                )
            }
        } else {
            PoolContent(accountBundle: signedInAccountBundle!, onSignOut: { signedInAccountBundle = nil })
        }
    }
}
