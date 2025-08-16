import SwiftUI
import Session
import Core
import Account

struct HomeRouter: View {
    let onSignIn: (AccountBundle) -> Void

    @State private var path = NavigationPath()
    @Environment(\.diResolver) var diResolver: DIResolver

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onSignInWithEmail: { path.append(EmailSignInRoute()) },
                onSignInWithEmailAndPassword: { path.append(EmailAndPasswordSignInRoute()) },
            )
            .navigationDestination(for: EmailSignInRoute.self) { route in
                EmailSignInView(
                    viewModel: EmailSignInViewModel(
                        sendSignInLinkToEmailUseCase: diResolver.resolve(SendSignInLinkToEmailUseCase.self)!
                    )
                )
            }
            .navigationDestination(for: EmailAndPasswordSignInRoute.self) { route in
                EmailAndPasswordSignInView(
                    viewModel: EmailAndPasswordSignInViewModel(
                        signInWithEmailAndPasswordUseCase: diResolver.resolve(SignInWithEmailAndPasswordUseCase.self)!
                    ),
                    onSignIn: onSignIn,
                )
            }
        }
    }
}
