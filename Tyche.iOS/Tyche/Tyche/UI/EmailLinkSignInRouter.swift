import SwiftUI
import Session
import Account

struct EmailLinkSignInRouter: View {
    var emailSignInURLProcessor: EmailSignInURLProcessor

    @Environment(\.diResolver) private var diResolver;
    @State private var signedInAccountBundle: AccountBundle? = nil
    
    init(emailSignInURLProcessor: EmailSignInURLProcessor) {
        self.emailSignInURLProcessor = emailSignInURLProcessor
    }
    
    var body: some View {
        if signedInAccountBundle == nil {
            NavigationStack {
                EmailLinkSignInView(
                    viewModel: EmailLinkSignInViewModel(
                        signInWithEmailLinkUseCase: diResolver.resolve(SignInWithEmailLinkUseCase.self)!
                    ),
                    email: emailSignInURLProcessor.email() ?? "",
                    emailLink: emailSignInURLProcessor.emailLink(),
                    onStartRequested: { accountBundle in
                        signedInAccountBundle = accountBundle
                    }
                )
            }
        } else {
            PoolContent(user: signedInAccountBundle!, onLogout: { signedInAccountBundle = nil })
        }
    }
}
